package com.swe574.group2.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.swe574.group2.backend.dao.BadgeRepository;
import com.swe574.group2.backend.dao.UserBadgeRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.BadgeDto;
import com.swe574.group2.backend.dto.UserProfileDto;
import com.swe574.group2.backend.dto.UserProfileUpdateDto;
import com.swe574.group2.backend.entity.Badge;
import com.swe574.group2.backend.entity.User;
import com.swe574.group2.backend.entity.UserBadge;
import com.swe574.group2.backend.exception.ResourceNotFoundException; // Corrected import path
import com.swe574.group2.backend.service.UserService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final Storage storage;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper; // For JSON handling of location

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           BadgeRepository badgeRepository,
                           UserBadgeRepository userBadgeRepository,
                           Storage storage,
                           ModelMapper modelMapper,
                           ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.storage = storage;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        UserProfileDto profileDto = modelMapper.map(user, UserProfileDto.class);

        // Map badges
        List<BadgeDto> badgeDtos = user.getUserBadges().stream()
                .map(userBadge -> modelMapper.map(userBadge.getBadge(), BadgeDto.class))
                .collect(Collectors.toList());
        profileDto.setBadges(badgeDtos);

        // Location is already a string in User entity, assuming it's stored as JSON
        // No extra conversion needed here for retrieval

        return profileDto;
    }

    @Override
    @Transactional
    public UserProfileDto updateUserProfile(Long userId, UserProfileUpdateDto updateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setBio(updateDto.getBio());

        // Convert location list to JSON string
        try {
            // Ensure location list is not null before processing
            List<String> locations = updateDto.getLocation() != null ? updateDto.getLocation() : Collections.emptyList();
            String locationJson = objectMapper.writeValueAsString(locations);
            user.setLocation(locationJson);
        } catch (JsonProcessingException e) {
            logger.error("Error converting location list to JSON for user {}: {}", userId, e.getMessage());
            // Handle error appropriately, maybe throw a custom exception
            throw new RuntimeException("Failed to update profile due to location processing error.", e);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // Return the updated profile
        return getUserProfile(updatedUser.getUsername());
    }

    @Override
    @Transactional
    public String updateProfilePicture(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile picture file cannot be empty.");
        }

        // Generate a unique filename for the GCS object
        String fileName = "profile-pictures/" + userId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        // Create a BlobId and BlobInfo for the file
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .setAcl(new ArrayList<>(Collections.singletonList(
                        Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))) // Public read access
                .build();

        // Upload the file to Google Cloud Storage
        storage.create(blobInfo, file.getBytes());

        // Generate the public URL for the uploaded file
        String fileUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);

        // Update the user's profile picture URL
        user.setProfilePictureUrl(fileUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return fileUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeDto> getUserBadges(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        List<UserBadge> userBadges = userBadgeRepository.findByUser_Id(userId);
        return userBadges.stream()
                .map(userBadge -> modelMapper.map(userBadge.getBadge(), BadgeDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeDto> getAllBadges() {
        List<Badge> badges = badgeRepository.findAll();
        return badges.stream()
                .map(badge -> modelMapper.map(badge, BadgeDto.class))
                .collect(Collectors.toList());
    }
}
