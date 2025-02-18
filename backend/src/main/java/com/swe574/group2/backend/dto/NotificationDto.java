package com.swe574.group2.backend.dto;

import lombok.Data;

@Data
public class NotificationDto {
    private Long id;
    private String message;
    private String type;
    private Long postId;
    private Long commentId;
    private Long userId;
    private boolean isRead;

    public NotificationDto() {
    }

    public NotificationDto(Long id, String message, String type, Long postId, Long commentId, Long userId, boolean isRead) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.postId = postId;
        this.commentId = commentId;
        this.userId = userId;
        this.isRead = isRead;
    }
}
