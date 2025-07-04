package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT DISTINCT new com.swe574.group2.backend.dto.PostListDto(p.id, user.username, p.title, p.description, mo.imageUrl, p.solved, p.upvotesCount, p.downvotesCount, (SELECT COUNT(c) FROM Comment c WHERE c.post = p)) "
            +
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
     * @Query("SELECT p.tags FROM Post p WHERE p.id = :postId")
     * Set<String> findTagsByPostId(@Param("postId") Long postId);
     */

    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN p.tagMap t " +
            "LEFT JOIN p.mysteryObject mo " +
            "WHERE FUNCTION('REGEXP_LIKE', LOWER(p.title), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(p.description), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(key(t)), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(value(t)), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.description), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.material), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.writtenText), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.color), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.shape), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.descriptionOfParts), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.location), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.hardness), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.timePeriod), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.smell), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.taste), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.texture), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.originOfAcquisition), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.pattern), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.brand), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.print), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.functionality), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.imageLicenses), CONCAT('\\b', LOWER(:keyword), '\\b')) = true OR " +
            "FUNCTION('REGEXP_LIKE', LOWER(mo.markings), CONCAT('\\b', LOWER(:keyword), '\\b')) = true")
    Page<Post> searchPosts(@Param("keyword") String keyword, Pageable pageable);

    List<Post> findByUserId(Long userId);

    @Query("SELECT key(t) FROM Post p JOIN p.tagMap t WHERE p.id = :postId")
    Set<String> findTagKeysByPostId(@Param("postId") Long postId);

    @Query("SELECT value(t) FROM Post p JOIN p.tagMap t WHERE p.id = :postId")
    Set<String> findTagLabelsByPostId(@Param("postId") Long postId);

    @Modifying
    @Query(value = "DELETE FROM post_contributing_comments WHERE comment_id = :commentId", nativeQuery = true)
    void removeCommentFromContributions(@Param("commentId") Long commentId);

}
