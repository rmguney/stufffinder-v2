package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.service.FollowedPostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/followed-posts")
public class FollowedPostController {

    private final FollowedPostService followedPostService;

    public FollowedPostController(FollowedPostService followedPostService) {
        this.followedPostService = followedPostService;
    }

    @PostMapping("/follow/{postId}")
    public ResponseEntity<Void> followPost(@PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        followedPostService.followPost(userDetails.getUsername(), postId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unfollow/{postId}")
    public ResponseEntity<Void> unfollowPost(@PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        followedPostService.unfollowPost(userDetails.getUsername(), postId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{postId}/followers-count")
    public ResponseEntity<Map<String, Integer>> getPostFollowerCount(@PathVariable Long postId) {

        int followerCount = followedPostService.getPostFollowerCount(postId);
        return ResponseEntity.ok(Map.of("followerCount", followerCount));
    }

    @GetMapping("/{postId}/is-following")
    public ResponseEntity<Map<String, Boolean>> isFollowing(@PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isFollowing = followedPostService.isFollowing(userDetails.getUsername(), postId);
        return ResponseEntity.ok(Map.of("isFollowing", isFollowing));
    }

}
