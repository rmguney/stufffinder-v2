package com.swe574.group2.backend.dto;

import com.swe574.group2.backend.enums.CommentType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDetailsDto {
    private Long id;
    private String content;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentDetailsDto> replies; // Nested replies
    private List<MediaFileDto> mediaFiles; // Added media files
    private CommentType commentType;

    private int upvotes;
    private int downvotes;

    private boolean userUpvoted;
    private boolean userDownvoted;

    private boolean isBestAnswer;

    private Long postId;

    public CommentDetailsDto(Long id, String content, String authorName, LocalDateTime createdAt, 
                           LocalDateTime updatedAt, List<CommentDetailsDto> replies, 
                           List<MediaFileDto> mediaFiles, int upvotes, int downvotes, 
                           boolean userUpvoted, boolean userDownvoted, boolean isBestAnswer, Long postId,
                           CommentType commentType) {
        this.id = id;
        this.content = content;
        this.author = authorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.replies = replies;
        this.mediaFiles = mediaFiles;
        this.upvotes = upvotes;
        this.downvotes = downvotes;
        this.userUpvoted = userUpvoted;
        this.userDownvoted = userDownvoted;
        this.isBestAnswer = isBestAnswer;
        this.postId = postId;
        this.commentType = commentType;
    }

    public CommentDetailsDto() {
    }
}