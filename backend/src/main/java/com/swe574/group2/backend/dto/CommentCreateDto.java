package com.swe574.group2.backend.dto;

import com.swe574.group2.backend.enums.CommentType;
import lombok.Data;

@Data
public class CommentCreateDto {
    private String content;
    private Long postId;
    private Long parentCommentId; // For replies, this can be null for root comments
    private CommentType commentType;
}

