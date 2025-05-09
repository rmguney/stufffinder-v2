package com.swe574.group2.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String username;
    private String email; // Consider if email should be public
    private String role;
    private String bio;
    private String profilePictureUrl;
    private String location; // JSON string representing country codes, e.g., ["TR", "US"]
    private List<BadgeDto> badges;
    private LocalDateTime createdAt;
    // Add other relevant fields if needed, e.g., reputation score later
}
