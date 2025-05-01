package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.service.FollowedUserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/followed-users")
public class FollowedUserController {

    private final FollowedUserService followedUserService;

    public FollowedUserController(FollowedUserService followedUserService) {
        this.followedUserService = followedUserService;
    }

    @PostMapping("/follow/{followedUsername}")
    public ResponseEntity<Void> followUser(@PathVariable String followedUsername,
            @AuthenticationPrincipal UserDetails userDetails) {

        followedUserService.followUser(userDetails.getUsername(), followedUsername);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unfollow/{followedUsername}")
    public ResponseEntity<Void> unfollowUser(@PathVariable String followedUsername,
            @AuthenticationPrincipal UserDetails userDetails) {
        followedUserService.unfollowUser(userDetails.getUsername(), followedUsername);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userName}/followers-count")
    public ResponseEntity<Map<String, Integer>> getFollowersCount(@PathVariable String userName) {
        int count = followedUserService.getFollowersCount(userName);
        return ResponseEntity.ok(Map.of("followerCount", count));
    }

    @GetMapping("/{userName}/following-count")
    public ResponseEntity<Map<String, Integer>> getFollowingCount(@PathVariable String userName) {
        int count = followedUserService.getFollowingCount(userName);
        return ResponseEntity.ok(Map.of("followingCount", count));
    }

    @GetMapping("/is-following/{followedUsername}")
    public ResponseEntity<Map<String, Boolean>> isFollowing(@PathVariable String followedUsername,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean following = followedUserService.isFollowing(userDetails.getUsername(), followedUsername);
        return ResponseEntity.ok(Map.of("isFollowing", following));
    }

}
