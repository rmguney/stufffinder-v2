package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.FollowerNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowerNotificationRepository extends JpaRepository<FollowerNotification, Long> {

    // Get all unread follower notifications for a user
    List<FollowerNotification> findByUserIdAndIsReadFalse(Long userId);
}

