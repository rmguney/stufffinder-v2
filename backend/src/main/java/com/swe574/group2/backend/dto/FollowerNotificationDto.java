package com.swe574.group2.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.swe574.group2.backend.enums.FollowerNotificationType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowerNotificationDto {
    private Long id;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private Long userId;
    private Long actorId;
    private Long postId;
    private Long commentId;
    private FollowerNotificationType type;
    }