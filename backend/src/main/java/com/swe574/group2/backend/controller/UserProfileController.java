package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.BadgeDto;
import com.swe574.group2.backend.dto.UserProfileDto;
import com.swe574.group2.backend.dto.UserProfileUpdateDto;
// import com.swe574.group2.backend.entity.User; // User entity not directly used here
import com.swe574.group2.backend.security.JwtUtil;
import com.swe574.group2.backend.service.UserService;
import io.jsonwebtoken.Claims; // Import Claims for extraction
import jakarta.servlet.http.HttpServletRequest; // To get the token
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users") // Base path for user-related operations
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil; // Inject JwtUtil to extract userId from token

    @Autowired
    public UserProfileController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // Get public user profile by username
    @GetMapping("/{username}/profile")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable String username) {
        try {
            UserProfileDto profile = userService.getUserProfile(username);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Error fetching profile for user {}: {}", username, e.getMessage());
            // Consider specific exception handling (e.g., ResourceNotFoundException)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found", e);
        }
    }

    // Update authenticated user's profile (bio, location)
    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()") // Ensure user is authenticated
    public ResponseEntity<UserProfileDto> updateUserProfile(@RequestBody UserProfileUpdateDto updateDto, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        try {
            UserProfileDto updatedProfile = userService.updateUserProfile(userId, updateDto);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            logger.error("Error updating profile for user {}: {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update profile", e);
        }
    }

    // Update authenticated user's profile picture
    @PostMapping("/me/profile-picture")
    @PreAuthorize("isAuthenticated()") // Ensure user is authenticated
    public ResponseEntity<Map<String, String>> updateProfilePicture(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        try {
            String fileUrl = userService.updateProfilePicture(userId, file);
            return ResponseEntity.ok(Map.of("profilePictureUrl", fileUrl));
        } catch (IOException e) {
            logger.error("Error uploading profile picture for user {}: {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload profile picture", e);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid profile picture upload attempt for user {}: {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during profile picture upload for user {}: {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", e);
        }
    }

    // Helper method to extract User ID from JWT token in request header
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header is missing or invalid");
        }
        String token = header.substring(7);
        try {
            // Extract claims and get the userId claim
            Claims claims = jwtUtil.extractClaims(token); // Now public
            Object userIdClaim = claims.get("userId");
            if (userIdClaim == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID not found in token");
            }
            // Claims might store it as Integer or Long depending on JWT library version/config
            if (userIdClaim instanceof Integer) {
                return ((Integer) userIdClaim).longValue();
            } else if (userIdClaim instanceof Long) {
                return (Long) userIdClaim;
            } else {
                 throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid User ID type in token");
            }
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token", e);
        }
    }

     // Endpoint to get all available badges (optional, maybe for admin)
     @GetMapping("/badges")
     public ResponseEntity<List<BadgeDto>> getAllBadges() {
         List<BadgeDto> badges = userService.getAllBadges();
         return ResponseEntity.ok(badges);
     }
}
