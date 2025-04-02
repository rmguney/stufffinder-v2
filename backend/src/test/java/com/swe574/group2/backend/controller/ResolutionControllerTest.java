package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.ResolutionCreateDto;
import com.swe574.group2.backend.dto.ResolutionDetailsDto;
import com.swe574.group2.backend.service.ResolutionService;
import com.swe574.group2.backend.entity.MediaFile;
import com.swe574.group2.backend.entity.Resolution;
import com.swe574.group2.backend.dao.ResolutionRepository;
import com.swe574.group2.backend.dao.MediaFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Storage;

@ExtendWith(MockitoExtension.class)
public class ResolutionControllerTest {

    @Mock
    private ResolutionService resolutionService;

    @Mock
    private ResolutionRepository resolutionRepository;

    @Mock
    private MediaFileRepository mediaFileRepository;
    
    @Mock
    private Storage storage;

    @InjectMocks
    private ResolutionController resolutionController;

    private UserDetails userDetails;
    private ResolutionCreateDto resolutionCreateDto;

    @BeforeEach
    public void setUp() {
        userDetails = User.withUsername("testuser")
                .password("password")
                .authorities("USER")
                .build();

        resolutionCreateDto = new ResolutionCreateDto();
        resolutionCreateDto.setPostId(1L);
        resolutionCreateDto.setDescription("Test resolution description");
    }

    // Test: Creating new resolution
    @Test
    public void testCreateResolution() {
        // Arrange
        Map<String, Long> expectedResponse = Map.of("resolutionId", 1L);
        when(resolutionService.createResolution(any(ResolutionCreateDto.class), anyString()))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<Map<String, Long>> response = resolutionController.createResolution(resolutionCreateDto, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("resolutionId", 1L);
        verify(resolutionService).createResolution(resolutionCreateDto, "testuser");
    }

    // Test: Getting resolutions when user is authenticated
    @Test
    public void testGetResolutionsForPost_AuthenticatedUser() {
        // Arrange
        Long postId = 1L;
        List<ResolutionDetailsDto> mockResolutions = Arrays.asList(
                new ResolutionDetailsDto(),
                new ResolutionDetailsDto()
        );
        when(resolutionService.getResolutionsForPost(postId, "testuser")).thenReturn(mockResolutions);

        // Act
        ResponseEntity<List<ResolutionDetailsDto>> response = resolutionController.getResolutionsForPost(postId, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(resolutionService).getResolutionsForPost(postId, "testuser");
    }

    // Test: Getting resolutions when user is not authenticated (null UserDetails)
    @Test
    public void testGetResolutionsForPost_UnauthenticatedUser() {
        // Arrange
        Long postId = 1L;
        List<ResolutionDetailsDto> mockResolutions = Arrays.asList(
                new ResolutionDetailsDto(),
                new ResolutionDetailsDto()
        );
        when(resolutionService.getResolutionsForPost(postId, null)).thenReturn(mockResolutions);

        // Act
        ResponseEntity<List<ResolutionDetailsDto>> response = resolutionController.getResolutionsForPost(postId, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(resolutionService).getResolutionsForPost(postId, null);
    }
}