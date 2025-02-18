package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:4200", "https://swe573-frontend-594781402587.us-central1.run.app"})
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"user", "replies", "upvotedBy", "downvotedBy"})
    List<Comment> findByPostId(Long postId);

    List<Comment> findByUserId(Long userId);
}
