package com.swe574.group2.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.swe574.group2.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private Storage storage;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Badge badge;
    private UserBadge userBadge;
    private UserProfileDto userProfileDto;
    private UserProfileUpdateDto userProfileUpdateDto;
    private BadgeDto badgeDto;

    private String bucketName = "test-bucket"; // Default for testing

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("testuser@example.com");
        user.setProfilePictureUrl("http://example.com/profile.jpg");
        user.setBio("Test bio");
        user.setLocation("[\"Test City\",\"Test Country\"]");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        badge = new Badge();
        badge.setId(1L);
        badge.setName("Test Badge");
        badge.setIconUrl("http://example.com/badge.png");

        userBadge = new UserBadge();
        userBadge.setUser(user);
        userBadge.setBadge(badge);
        // userBadge.setEarnedAt(LocalDateTime.now()); // Removed

        user.setUserBadges(Set.of(userBadge));

        userProfileDto = new UserProfileDto();
        userProfileDto.setId(user.getId());
        userProfileDto.setUsername(user.getUsername());
        userProfileDto.setProfilePictureUrl(user.getProfilePictureUrl());
        userProfileDto.setBio(user.getBio());
        userProfileDto.setLocation(user.getLocation()); // Corrected: Pass the JSON string directly

        badgeDto = new BadgeDto();
        badgeDto.setId(badge.getId());
        badgeDto.setName(badge.getName());
        badgeDto.setIconUrl(badge.getIconUrl());
        userProfileDto.setBadges(List.of(badgeDto));

        userProfileUpdateDto = new UserProfileUpdateDto();
        userProfileUpdateDto.setBio("Updated bio");
        userProfileUpdateDto.setLocation(List.of("Updated City", "Updated Country"));

        // Set bucketName via reflection for testing as @Value won't work in plain JUnit
        try {
            java.lang.reflect.Field field = UserServiceImpl.class.getDeclaredField("bucketName");
            field.setAccessible(true);
            field.set(userService, bucketName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    void getUserProfile_UserExists_ReturnsUserProfileDto() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserProfileDto.class)).thenReturn(userProfileDto);
        when(modelMapper.map(badge, BadgeDto.class)).thenReturn(badgeDto);

        // Act
        UserProfileDto result = userService.getUserProfile("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(userProfileDto.getId(), result.getId());
        assertEquals(userProfileDto.getUsername(), result.getUsername());
        assertEquals(userProfileDto.getBio(), result.getBio());
        assertEquals(userProfileDto.getLocation(), result.getLocation());
        assertNotNull(result.getBadges());
        assertFalse(result.getBadges().isEmpty());
        assertEquals(badgeDto.getName(), result.getBadges().get(0).getName());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(modelMapper, times(1)).map(user, UserProfileDto.class);
        verify(modelMapper, times(user.getUserBadges().size())).map(any(Badge.class), eq(BadgeDto.class));
    }

    @Test
    void getUserProfile_UserNotExists_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserProfile("nonexistentuser");
        });
        verify(userRepository, times(1)).findByUsername("nonexistentuser");
    }

    @Test
    void updateUserProfile_UserExists_ReturnsUpdatedUserProfileDto() throws JsonProcessingException {
        // Arrange
        String updatedLocationJson = "[\"Updated City\",\"Updated Country\"]";
        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setBio(userProfileUpdateDto.getBio());
        updatedUser.setLocation(updatedLocationJson);
        updatedUser.setProfilePictureUrl(user.getProfilePictureUrl()); // Assuming this doesn't change here
        updatedUser.setUserBadges(user.getUserBadges());


        UserProfileDto updatedProfileDto = new UserProfileDto();
        updatedProfileDto.setId(updatedUser.getId());
        updatedProfileDto.setUsername(updatedUser.getUsername());
        updatedProfileDto.setBio(updatedUser.getBio());
        updatedProfileDto.setLocation(updatedUser.getLocation());
        updatedProfileDto.setBadges(List.of(badgeDto)); // Assuming badges don't change

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(userProfileUpdateDto.getLocation())).thenReturn(updatedLocationJson);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Mocking the subsequent call to getUserProfile
        when(userRepository.findByUsername(updatedUser.getUsername())).thenReturn(Optional.of(updatedUser));
        when(modelMapper.map(updatedUser, UserProfileDto.class)).thenReturn(updatedProfileDto);
        // Assuming badge mapping is consistent
        when(modelMapper.map(any(Badge.class), eq(BadgeDto.class))).thenReturn(badgeDto);


        // Act
        UserProfileDto result = userService.updateUserProfile(user.getId(), userProfileUpdateDto);

        // Assert
        assertNotNull(result);
        assertEquals(userProfileUpdateDto.getBio(), result.getBio());
        assertEquals(updatedLocationJson, result.getLocation()); // Location is JSON string in DTO
        verify(userRepository, times(1)).findById(user.getId());
        verify(objectMapper, times(1)).writeValueAsString(userProfileUpdateDto.getLocation());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository, times(1)).findByUsername(updatedUser.getUsername()); // For subsequent getUserProfile
    }

    @Test
    void updateUserProfile_UserNotExists_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUserProfile(99L, userProfileUpdateDto);
        });
        verify(userRepository, times(1)).findById(99L);
    }

    @Test
    void updateUserProfile_JsonProcessingException_ThrowsRuntimeException() throws JsonProcessingException {
        // Arrange
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(userProfileUpdateDto.getLocation())).thenThrow(JsonProcessingException.class);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.updateUserProfile(user.getId(), userProfileUpdateDto);
        });
        verify(userRepository, times(1)).findById(user.getId());
        verify(objectMapper, times(1)).writeValueAsString(userProfileUpdateDto.getLocation());
    }

    @Test
    void updateUserProfile_NullLocation_UsesEmptyList() throws JsonProcessingException {
        // Arrange
        userProfileUpdateDto.setLocation(null);
        String emptyLocationJson = "[]";

        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setBio(userProfileUpdateDto.getBio());
        updatedUser.setLocation(emptyLocationJson);
        updatedUser.setProfilePictureUrl(user.getProfilePictureUrl());
        updatedUser.setUserBadges(user.getUserBadges());

        UserProfileDto updatedProfileDto = new UserProfileDto();
        updatedProfileDto.setId(updatedUser.getId());
        updatedProfileDto.setUsername(updatedUser.getUsername());
        updatedProfileDto.setBio(updatedUser.getBio());
        updatedProfileDto.setLocation(emptyLocationJson);
        updatedProfileDto.setBadges(List.of(badgeDto));


        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(Collections.emptyList())).thenReturn(emptyLocationJson);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        when(userRepository.findByUsername(updatedUser.getUsername())).thenReturn(Optional.of(updatedUser));
        when(modelMapper.map(updatedUser, UserProfileDto.class)).thenReturn(updatedProfileDto);
        when(modelMapper.map(any(Badge.class), eq(BadgeDto.class))).thenReturn(badgeDto);


        // Act
        UserProfileDto result = userService.updateUserProfile(user.getId(), userProfileUpdateDto);

        // Assert
        assertNotNull(result);
        assertEquals(emptyLocationJson, result.getLocation());
        verify(objectMapper, times(1)).writeValueAsString(Collections.emptyList());
    }

    @Test
    void updateProfilePicture_UserExists_FileValid_ReturnsUrl() throws IOException {
        // Arrange
        Long userId = 1L;
        String originalFilename = "profile.jpg";
        String contentType = "image/jpeg";
        byte[] fileContent = "test-image-data".getBytes();
        String expectedFileNameRegex = "profile-pictures/" + userId + "/[a-f0-9\\-]+" + "-" + originalFilename;
        String expectedUrl = String.format("https://storage.googleapis.com/%s/profile-pictures/%s/%s-%s",
                bucketName, userId, "uuid", originalFilename); // Simplified for assertion matching

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn(originalFilename);
        when(mockFile.getContentType()).thenReturn(contentType);
        when(mockFile.getBytes()).thenReturn(fileContent);
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenAnswer(invocation -> {
            BlobInfo blobInfo = invocation.getArgument(0);
            // For robust matching, we check parts of the blobId name
            assertTrue(blobInfo.getBlobId().getName().matches(expectedFileNameRegex));
            assertEquals(bucketName, blobInfo.getBlobId().getBucket());
            assertEquals(contentType, blobInfo.getContentType());
            assertNotNull(blobInfo.getAcl());
            // Acl.User.ofAllUsers() and Acl.Role.READER might need specific mock or verification
            return null; // storage.create returns Blob, but we don't use it here
        });
        when(userRepository.save(any(User.class))).thenReturn(user);


        // Act
        String resultUrl = userService.updateProfilePicture(userId, mockFile);

        // Assert
        assertNotNull(resultUrl);
        assertTrue(resultUrl.startsWith(String.format("https://storage.googleapis.com/%s/profile-pictures/%s/", bucketName, userId)));
        assertTrue(resultUrl.endsWith("-" + originalFilename));

        verify(userRepository, times(1)).findById(userId);
        verify(mockFile, times(1)).getBytes();
        verify(storage, times(1)).create(any(BlobInfo.class), eq(fileContent));
        verify(userRepository, times(1)).save(user);
        assertNotNull(user.getProfilePictureUrl());
        assertTrue(user.getProfilePictureUrl().matches(String.format("https://storage.googleapis.com/%s/%s", bucketName, expectedFileNameRegex)));
    }

    @Test
    void updateProfilePicture_UserNotExists_ThrowsResourceNotFoundException() throws IOException {
        // Arrange
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateProfilePicture(userId, mockFile);
        });
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void updateProfilePicture_FileNull_ThrowsIllegalArgumentException() throws IOException {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateProfilePicture(userId, null);
        });
    }

    @Test
    void updateProfilePicture_FileEmpty_ThrowsIllegalArgumentException() throws IOException {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mockFile.isEmpty()).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateProfilePicture(userId, mockFile);
        });
    }

    @Test
    void updateProfilePicture_IOExceptionOnGetbytes_ThrowsIOException() throws IOException {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("profile.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getBytes()).thenThrow(IOException.class);

        // Act & Assert
        assertThrows(IOException.class, () -> {
            userService.updateProfilePicture(userId, mockFile);
        });
    }

    @Test
    void getUserBadges_UserExists_HasBadges_ReturnsBadgeDtoList() {
        // Arrange
        Long userId = 1L;
        List<UserBadge> userBadgesList = List.of(userBadge);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userBadgeRepository.findByUser_Id(userId)).thenReturn(userBadgesList);
        when(modelMapper.map(badge, BadgeDto.class)).thenReturn(badgeDto);

        // Act
        List<BadgeDto> result = userService.getUserBadges(userId);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(badgeDto.getName(), result.get(0).getName());
        verify(userRepository, times(1)).existsById(userId);
        verify(userBadgeRepository, times(1)).findByUser_Id(userId);
        verify(modelMapper, times(1)).map(badge, BadgeDto.class);
    }

    @Test
    void getUserBadges_UserExists_NoBadges_ReturnsEmptyList() {
        // Arrange
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userBadgeRepository.findByUser_Id(userId)).thenReturn(Collections.emptyList());

        // Act
        List<BadgeDto> result = userService.getUserBadges(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).existsById(userId);
        verify(userBadgeRepository, times(1)).findByUser_Id(userId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void getUserBadges_UserNotExists_ThrowsResourceNotFoundException() {
        // Arrange
        Long userId = 99L;
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserBadges(userId);
        });
        verify(userRepository, times(1)).existsById(userId);
        verify(userBadgeRepository, never()).findByUser_Id(anyLong());
    }

    @Test
    void getAllBadges_BadgesExist_ReturnsBadgeDtoList() {
        // Arrange
        List<Badge> badgesList = List.of(badge);
        when(badgeRepository.findAll()).thenReturn(badgesList);
        when(modelMapper.map(badge, BadgeDto.class)).thenReturn(badgeDto);

        // Act
        List<BadgeDto> result = userService.getAllBadges();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(badgeDto.getName(), result.get(0).getName());
        verify(badgeRepository, times(1)).findAll();
        verify(modelMapper, times(1)).map(badge, BadgeDto.class);
    }

    @Test
    void getAllBadges_NoBadgesExist_ReturnsEmptyList() {
        // Arrange
        when(badgeRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<BadgeDto> result = userService.getAllBadges();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(badgeRepository, times(1)).findAll();
        verify(modelMapper, never()).map(any(), any());
    }
}
