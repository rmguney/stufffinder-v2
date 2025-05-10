package com.swe574.group2.backend.service.impl;

import com.swe574.group2.backend.dao.ReportRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.MediaFileRepository;
import com.swe574.group2.backend.dao.NotificationRepository;
import com.swe574.group2.backend.dto.ReportRequestDto;
import com.swe574.group2.backend.dto.ReportResponseDto;
import com.swe574.group2.backend.entity.Notification;
import com.swe574.group2.backend.entity.Report;
import com.swe574.group2.backend.entity.Role;
import com.swe574.group2.backend.entity.User;
import com.swe574.group2.backend.enums.ReportStatus;
import com.swe574.group2.backend.enums.TargetType;
import com.swe574.group2.backend.service.ReportService;

import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final MediaFileRepository mediaFileRepository;

    public ReportServiceImpl(
            ReportRepository reportRepository,
            UserRepository userRepository,
            PostRepository postRepository,
            CommentRepository commentRepository,
            NotificationRepository notificationRepository,
            MediaFileRepository mediaFileRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.notificationRepository = notificationRepository;
        this.mediaFileRepository = mediaFileRepository;
    }

    @Override
    @Transactional
    public ReportResponseDto submitReport(ReportRequestDto request, String reporterEmail) {
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new RuntimeException("Reporting user not found"));
        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .notes(request.getNotes())
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
        report = reportRepository.save(report);

        List<User> admins = userRepository.findAllAdmins();
        for (User admin : admins) {
            Notification adminNotification = new Notification();
            adminNotification.setUser(admin);
            adminNotification.setType(Notification.NotificationType.REPORT);
            adminNotification.setMessage("A new report has been submitted and requires review.");
            notificationRepository.save(adminNotification);
        }
        return mapToDto(report);
    }

    @Override
    public Page<ReportResponseDto> listReports(Pageable pageable) {
        return reportRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public ReportResponseDto resolveReport(Long reportId,
            com.swe574.group2.backend.dto.ReportResolutionDto resolutionDto, String adminEmail) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        String action = resolutionDto.getAction();
        if ("RESOLVED".equalsIgnoreCase(action)) {
            report.setStatus(ReportStatus.RESOLVED);
        } else if ("DISMISSED".equalsIgnoreCase(action)) {
            report.setStatus(ReportStatus.DISMISSED);
        } else {
            throw new IllegalArgumentException("Invalid resolve action: " + action);
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());

        // Set resolution note if provided
        if (resolutionDto.getResolutionNote() != null) {
            report.setNotes(report.getNotes() == null
                    ? resolutionDto.getResolutionNote()
                    : report.getNotes() + "\n[RESOLUTION] " + resolutionDto.getResolutionNote());
        }

        // Moderation actions
        User reportedUser = null;
        if (report.getTargetType() != null && report.getTargetId() != null) {
            switch (report.getTargetType()) {
                case POST:
                    if (resolutionDto.isRemoveContent()) {
                        postRepository.findById(report.getTargetId()).ifPresent(post -> {
                            if (post.getMysteryObject() != null) {
                                Long mysteryObjectId = post.getMysteryObject().getId();
                                mediaFileRepository
                                        .deleteAll(mediaFileRepository.findByMysteryObjectId(mysteryObjectId));
                            }
                        });

                        postRepository.deleteById(report.getTargetId());
                    }

                    com.swe574.group2.backend.entity.Post post = postRepository.findById(report.getTargetId())
                            .orElse(null);
                    if (post != null) {
                        reportedUser = post.getUser();
                    }
                    break;
                case COMMENT:
                    if (resolutionDto.isRemoveContent()) {
                        postRepository.removeCommentFromContributions(report.getTargetId());
                        notificationRepository.deleteByCommentId(report.getTargetId());
                        commentRepository.deleteById(report.getTargetId());

                        com.swe574.group2.backend.entity.Comment deletedComment = commentRepository
                                .findById(report.getTargetId()).orElse(null);
                        if (deletedComment != null && deletedComment.getPost() != null) {
                            Long postId = deletedComment.getPost().getId();
                            com.swe574.group2.backend.entity.Post postToCheck = postRepository.findById(postId)
                                    .orElse(null);
                            if (postToCheck != null && postToCheck.isSolved()
                                    && (postToCheck.getContributingComments() == null
                                            || postToCheck.getContributingComments().isEmpty())) {
                                postToCheck.setSolved(false);
                                postToCheck.setResolvedAt(null);
                                postToCheck.setResolutionDescription(null);
                                postRepository.save(postToCheck);
                            }
                        }
                    }
                    com.swe574.group2.backend.entity.Comment comment = commentRepository.findById(report.getTargetId())
                            .orElse(null);
                    if (comment != null) {
                        reportedUser = comment.getUser();
                    }
                    break;
                case PROFILE:
                    if (resolutionDto.isRemoveContent()) {
                        userRepository.deleteById(report.getTargetId());
                    }
                    reportedUser = userRepository.findById(report.getTargetId()).orElse(null);
                    break;
                default:
                    break;
            }
        }

        // Ban or suspend the reported user
        if (resolutionDto.isBanUser() && reportedUser != null) {
            reportedUser.setRole(com.swe574.group2.backend.entity.Role.BANNED);
            userRepository.save(reportedUser);
        }

        // Send warning notification to the reported user
        if (resolutionDto.isSendWarning() && reportedUser != null) {
            com.swe574.group2.backend.entity.Notification notification = new com.swe574.group2.backend.entity.Notification();
            notification.setUser(reportedUser);
            notification.setType(com.swe574.group2.backend.entity.Notification.NotificationType.REPORT);
            notification.setMessage(
                    "You have received a warning due to a report. Please review your content and adhere to community guidelines.");

            if (report.getTargetType() != null) {
                switch (report.getTargetType()) {
                    case POST:
                        com.swe574.group2.backend.entity.Post post = postRepository.findById(report.getTargetId())
                                .orElse(null);
                        if (post != null) {
                            notification.setPost(post);
                        }
                        break;
                    case COMMENT:
                        com.swe574.group2.backend.entity.Comment comment = commentRepository
                                .findById(report.getTargetId()).orElse(null);
                        if (comment != null) {
                            notification.setComment(comment);
                        }
                        break;
                    case PROFILE:
                        break;
                    default:
                        break;
                }
            }

            // Save the notification
            notificationRepository.save(notification);
        }

        report.setRemoveContent(resolutionDto.isRemoveContent());
        report.setBanUser(resolutionDto.isBanUser());
        report.setSendWarning(resolutionDto.isSendWarning());

        report = reportRepository.save(report);
        return mapToDto(report);
    }

    private ReportResponseDto mapToDto(Report report) {
        ReportResponseDto dto = new ReportResponseDto();
        dto.setId(report.getId());
        dto.setReporterUsername(report.getReporter().getUsername());
        dto.setTargetType(report.getTargetType());
        dto.setTargetId(report.getTargetId());
        dto.setReason(report.getReason());
        dto.setNotes(report.getNotes());
        dto.setStatus(report.getStatus());
        dto.setCreatedAt(report.getCreatedAt());

        if (report.getTargetType() == TargetType.PROFILE && report.getTargetId() != null) {
            userRepository.findById(report.getTargetId())
                    .ifPresent(user -> dto.setReportedUsername(user.getUsername()));
        }
        if (report.getTargetType() == TargetType.COMMENT && report.getTargetId() != null) {
            commentRepository.findById(report.getTargetId())
                    .ifPresent(comment -> dto.setPostId(comment.getPost().getId()));
        }

        if (report.getResolvedBy() != null) {
            dto.setResolvedByUsername(report.getResolvedBy().getUsername());
        }
        dto.setResolvedAt(report.getResolvedAt());
        dto.setRemoveContent(report.getRemoveContent());
        dto.setBanUser(report.getBanUser());
        dto.setSendWarning(report.getSendWarning());

        return dto;
    }

    @Override
    public List<ReportResponseDto> listReportByTarget(String targetType, Long id) {

        TargetType parsedTargetType;
        try {
            parsedTargetType = TargetType.valueOf(targetType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid targetType: " + targetType);
        }

        List<Report> reports = reportRepository.findAll();
        List<Report> filteredReports = reports.stream()
                .filter(r -> r.getTargetType() == parsedTargetType && r.getTargetId().equals(id))
                .collect(Collectors.toList());

        List<ReportResponseDto> mapList = filteredReports.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return mapList;
    }
}
