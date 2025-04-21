package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.UserBadge;
import com.swe574.group2.backend.entity.UserBadgeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UserBadgeId> {
    List<UserBadge> findByUser_Id(Long userId);
    List<UserBadge> findByBadge_Id(Long badgeId);
}
