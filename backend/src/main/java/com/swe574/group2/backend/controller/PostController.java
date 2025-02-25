package com.swe574.group2.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swe574.group2.backend.dto.PostCreationDto;
import com.swe574.group2.backend.dto.PostDetailsDto;
import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.entity.MysteryObject;
import com.swe574.group2.backend.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Long>> createPost(
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "tags", required = false) List<String> tags,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart("mysteryObject") String mysteryObjectJson,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        
        ObjectMapper objectMapper = new ObjectMapper();
        MysteryObject mysteryObject = objectMapper.readValue(mysteryObjectJson, MysteryObject.class);
        
        PostCreationDto postCreationDto = new PostCreationDto();
        postCreationDto.setTitle(title);
        postCreationDto.setContent(content);
        postCreationDto.setTags(new HashSet<>(tags != null ? tags : Collections.emptyList()));
        postCreationDto.setImage(image);
        postCreationDto.setMysteryObject(mysteryObject);
        
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
