package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.FollowerNotificationDto;
import com.swe574.group2.backend.service.FollowerNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follower-notifications")
public class FollowerNotificationController {

    private final FollowerNotificationService followerNotificationService;

    public FollowerNotificationController(FollowerNotificationService followerNotificationService) {
        this.followerNotificationService = followerNotificationService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<FollowerNotificationDto>> getFollowerNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(followerNotificationService.getFollowerNotifications(userId));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        followerNotificationService.markFollowerNotificationAsRead(notificationId);
        return ResponseEntity.ok().build();
    }
}
