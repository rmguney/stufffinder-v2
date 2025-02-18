package com.swe574.group2.backend.dto;

import lombok.Data;

@Data
public class CommentCreateDto {
    private String content;
    private Long postId;
    private Long parentCommentId; // For replies, this can be null for root comments
}

