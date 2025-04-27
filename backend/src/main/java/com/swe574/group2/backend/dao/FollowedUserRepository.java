package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.FollowedUser;
import com.swe574.group2.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowedUserRepository extends JpaRepository<FollowedUser, Long> {

    // All users followed by a given user
    List<FollowedUser> findByFollower(User follower);

    // All users following a given user
    List<FollowedUser> findByFollowed(User followed);

    // Check if a user follows another user
    Optional<FollowedUser> findByFollowerAndFollowed(User follower, User followed);

    int countByFollower(User follower);

    int countByFollowed(User followed);
}
