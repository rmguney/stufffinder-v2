package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResolutionRepository extends JpaRepository<Resolution, Long> {
    
    // Find all resolutions for a specific post
    List<Resolution> findByPostId(Long postId);

}
