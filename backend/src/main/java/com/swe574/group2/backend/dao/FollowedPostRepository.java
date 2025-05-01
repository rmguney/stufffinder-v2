package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.FollowedPost;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowedPostRepository extends JpaRepository<FollowedPost, Long> {

    // All posts followed by a user
    List<FollowedPost> findByFollower(User follower);

    // All users who follow a specific post
    List<FollowedPost> findByPost(Post post);

    // Check if a user is following a post
    Optional<FollowedPost> findByFollowerAndPost(User follower, Post post);
    
    int countByPost(Post post);

}
