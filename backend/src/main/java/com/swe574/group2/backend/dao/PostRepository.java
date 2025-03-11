package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface PostRepository  extends JpaRepository<Post, Long> {
    @Query("SELECT DISTINCT new com.swe574.group2.backend.dto.PostListDto(p.id, user.username, p.title, p.description, mo.image, p.solved) " +
            "FROM Post p " +
            "LEFT JOIN p.mysteryObject mo " +
            "LEFT JOIN p.user user")
    Page<PostListDto> findAllForPostList(Pageable pageable);

    @Query("SELECT p " +
            "FROM Post p " +
            "LEFT JOIN FETCH p.mysteryObject mo " +
            "WHERE p.id = :id")
    Post findPostDetailsById(Long id);

/*
    @Query("SELECT p.tags FROM Post p WHERE p.id = :postId")
    Set<String> findTagsByPostId(@Param("postId") Long postId);
*/

    @Query("""
                SELECT DISTINCT p FROM Post p 
                LEFT JOIN p.tagMap t 
                LEFT JOIN p.mysteryObject mo 
                WHERE 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.title, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.description, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(key(t), ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(value(t), ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.description, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.material, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.writtenText, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.color, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.shape, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.location, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.hardness, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.timePeriod, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.smell, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.texture, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.originOfAcquisition, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.pattern, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.print, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR 
                CONCAT(' ', LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(mo.functionality, ',', ' '), '.', ' '), '!', ' '), '?', ' '), ':', ' '), ';', ' ')), ' ') LIKE CONCAT('% ', LOWER(:keyword), ' %') OR
                LOWER(p.title) = LOWER(:keyword) OR 
                LOWER(p.description) = LOWER(:keyword) OR 
                LOWER(key(t)) = LOWER(:keyword) OR 
                LOWER(value(t)) = LOWER(:keyword) OR
                LOWER(mo.description) = LOWER(:keyword) OR
                LOWER(mo.material) = LOWER(:keyword) OR
                LOWER(mo.writtenText) = LOWER(:keyword) OR
                LOWER(mo.color) = LOWER(:keyword) OR
                LOWER(mo.shape) = LOWER(:keyword) OR
                LOWER(mo.location) = LOWER(:keyword) OR
                LOWER(mo.hardness) = LOWER(:keyword) OR
                LOWER(mo.timePeriod) = LOWER(:keyword) OR
                LOWER(mo.smell) = LOWER(:keyword) OR
                LOWER(mo.texture) = LOWER(:keyword) OR
                LOWER(mo.originOfAcquisition) = LOWER(:keyword) OR
                LOWER(mo.pattern) = LOWER(:keyword) OR
                LOWER(mo.print) = LOWER(:keyword) OR
                LOWER(mo.functionality) = LOWER(:keyword)
                """)
    Page<Post> searchPosts(@Param("keyword") String keyword, Pageable pageable);

    List<Post> findByUserId(Long userId);

    @Query("SELECT key(t) FROM Post p JOIN p.tagMap t WHERE p.id = :postId")
    Set<String> findTagKeysByPostId(@Param("postId") Long postId);

    @Query("SELECT value(t) FROM Post p JOIN p.tagMap t WHERE p.id = :postId")
    Set<String> findTagLabelsByPostId(@Param("postId") Long postId);
}
