package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"user", "replies", "upvotedBy", "downvotedBy"})
    List<Comment> findByPostId(Long postId);

    List<Comment> findByUserId(Long userId);
}
