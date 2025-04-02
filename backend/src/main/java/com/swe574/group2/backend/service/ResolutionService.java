package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.ResolutionRepository;
import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.MediaFileRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.ResolutionCreateDto;
import com.swe574.group2.backend.dto.ResolutionDetailsDto;
import com.swe574.group2.backend.dto.MediaFileDto;
import com.swe574.group2.backend.entity.Resolution;
import com.swe574.group2.backend.entity.MediaFile;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResolutionService {

    private final ResolutionRepository resolutionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final MediaFileRepository mediaFileRepository;

    public ResolutionService(ResolutionRepository resolutionRepository, PostRepository postRepository, 
                         UserRepository userRepository,
                         MediaFileRepository mediaFileRepository, CommentRepository commentRepository) {
        this.resolutionRepository = resolutionRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public Map<String, Long> createResolution(ResolutionCreateDto resolutionCreateDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Post post = postRepository.findById(resolutionCreateDto.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + resolutionCreateDto.getPostId()));

        List<Long> commentIds = resolutionCreateDto.getComments();
        List<Comment> comments = commentRepository.findAllById(commentIds);

        if (comments.size() != commentIds.size()) {
            throw new RuntimeException("Some comment IDs are invalid.");
        }

        Resolution resolution = new Resolution();
        resolution.setDescription(resolutionCreateDto.getDescription());
        resolution.setUser(user);
        resolution.setPost(post);

        // Save resolution
        resolution = resolutionRepository.save(resolution);

        // Update the comments' solved status
        comments.forEach(comment -> comment.setSolving(true));

        // Save updated comments
        commentRepository.saveAll(comments);

        // Update the post's solved status
        post.setSolved(true);
        postRepository.save(post); // Save the updated post

        Map<String, Long> response = new HashMap<>();
        response.put("resolutionId", resolution.getId());

        return response;
    }

    public List<ResolutionDetailsDto> getResolutionsForPost(Long postId, String email) {
        List<Resolution> resolutions = resolutionRepository.findByPostId(postId);
        User user = email != null ? userRepository.findByEmail(email).orElseThrow() : null;

        List<ResolutionDetailsDto> resolutionDetailsDtos = new ArrayList<>();
        for (var resolution : resolutions) {
                resolutionDetailsDtos.add(mapResolutionToDto(resolution, user));
        }

        return resolutionDetailsDtos;
    }

    private ResolutionDetailsDto mapResolutionToDto(Resolution resolution, User currentUser) {

        // Get media files for this resolution
        List<MediaFileDto> mediaFileDtos = mediaFileRepository.findByResolutionId(resolution.getId())
            .stream()
            .map(mediaFile -> {
                MediaFileDto dto = new MediaFileDto();
                dto.setId(mediaFile.getId());
                dto.setFileName(mediaFile.getFileName());
                dto.setFileType(mediaFile.getFileType());
                dto.setCreatedAt(mediaFile.getCreatedAt());
                return dto;
            })
            .collect(Collectors.toList());

        return new ResolutionDetailsDto(
                resolution.getId(),
                resolution.getDescription(),
                resolution.getCreatedAt(),
                resolution.getUpdatedAt(),
                mediaFileDtos,
                resolution.getPost().getId()
        );
    }
}