package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

}
