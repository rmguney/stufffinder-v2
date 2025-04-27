package com.swe574.group2.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.swe574.group2.backend.enums.FollowerNotificationType;

@Entity
@Table(name = "follower_notifications")
@Data
public class FollowerNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // The user who receives the notification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Optional: the post that triggered the notification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    // Optional: the comment that triggered the notification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    // Optional: the user who triggered the notification (e.g., created a post you follow)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowerNotificationType type;
}
