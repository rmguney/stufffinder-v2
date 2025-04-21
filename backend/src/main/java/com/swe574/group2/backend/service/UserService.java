package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dto.BadgeDto;
import com.swe574.group2.backend.dto.UserProfileDto;
import com.swe574.group2.backend.dto.UserProfileUpdateDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface UserService {
    UserProfileDto getUserProfile(String username);
    UserProfileDto updateUserProfile(Long userId, UserProfileUpdateDto updateDto);
    String updateProfilePicture(Long userId, MultipartFile file) throws IOException;
    List<BadgeDto> getUserBadges(Long userId);
    List<BadgeDto> getAllBadges();
    // Add methods for assigning/removing badges later if needed
}
