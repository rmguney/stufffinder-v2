package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.PostCreationDto;
import com.swe574.group2.backend.dto.PostDetailsDto;
import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {"http://localhost:4200", "https://swe573-frontend-594781402587.us-central1.run.app"})
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(value = "/create")
    public ResponseEntity<Map<String, Long>> createPost(
            @RequestPart("post") PostCreationDto postCreationDto,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        Map<String, Long> response = postService.createPost(postCreationDto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/getForPostList")
    public Page<PostListDto> getPostsForPostList(Pageable pageable) {
        return postService.getAllPostsForPostList(pageable);
    }

    @GetMapping("/getForPostDetails/{postId}")
    public ResponseEntity<PostDetailsDto> getPostDetails(@PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        PostDetailsDto postDetailsDto = postService.getPostDetails(postId, username);
        return ResponseEntity.ok(postDetailsDto);
    }

    @PostMapping("/upvote/{postId}")
    public ResponseEntity<Map<String, Long>> upvotePost(@PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Long> response = postService.upvotePost(postId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/downvote/{postId}")
    public ResponseEntity<Map<String, Long>> downvotePost(@PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Long> response = postService.downvotePost(postId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}/markBestAnswer/{commentId}")
    public ResponseEntity<Map<String, Long>> markBestAnswer(@PathVariable Long postId, @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        boolean success = postService.markBestAnswer(postId, commentId, userDetails.getUsername());
        if (success) {
            Map<String, Long> response = Map.of("postId", postId, "commentId", commentId);
            return ResponseEntity.ok(response);
        } else {
            Map<String, Long> response = Map.of("postId", postId, "commentId", commentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    @GetMapping("/searchForPosts")
    public ResponseEntity<Page<PostListDto>> searchPosts(@RequestParam("q") String query, Pageable pageable) {
        Page<PostListDto> searchResults = postService.searchPosts(query, pageable);
        return ResponseEntity.ok(searchResults);
    }
}
