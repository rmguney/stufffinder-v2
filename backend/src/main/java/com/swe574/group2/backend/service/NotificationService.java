package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.NotificationRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.NotificationDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.Notification;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void sendCommentNotification(Long userId, Post post, Comment comment) {
        User user = userRepository.findById(userId).orElseThrow();
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage("You received a new comment.");
        notification.setType(Notification.NotificationType.COMMENT);
        notification.setPost(post);
        notification.setComment(comment);
        notificationRepository.save(notification);
    }

    public void sendUpvoteNotification(Long userId, Comment comment) {
        User user = userRepository.findById(userId).orElseThrow();
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage("You received an upvote.");
        notification.setType(Notification.NotificationType.UPVOTE);
        notification.setComment(comment);
        notification.setPost(comment.getPost());
        notificationRepository.save(notification);
    }

    public void sendBestAnswerNotification(Long userId, Post post, Comment comment) {
        User user = userRepository.findById(userId).orElseThrow();
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage("Your comment was marked as the best answer.");
        notification.setType(Notification.NotificationType.BEST_ANSWER);
        notification.setPost(post);
        notification.setComment(comment);
        notificationRepository.save(notification);
    }

    public List<NotificationDto> getUserNotifications(Long userId) {
        List<NotificationDto> response =
                notificationRepository.findByUserIdAndIsReadFalse(userId).stream()
                        .map(notification -> new NotificationDto(notification.getId(), notification.getMessage(), notification.getType().name(), notification.getPost().getId(), notification.getComment().getId(), notification.getUser().getId(), notification.isRead()))
                        .toList();
        return response;
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow();
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
