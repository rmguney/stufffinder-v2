package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.NotificationDto;
import com.swe574.group2.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private List<NotificationDto> mockNotifications;

    @BeforeEach
    public void setUp() {
        mockNotifications = Arrays.asList(
                new NotificationDto(
                        1L,
                        "Test Notification 1",
                        "COMMENT",
                        2L,
                        3L,
                        4L,
                        false
                ),
                new NotificationDto(
                        2L,
                        "Test Notification 2",
                        "POST",
                        5L,
                        null,
                        6L,
                        true
                )
        );
    }

    @Test
    public void testGetUserNotifications_Success() {
        // Arrange
        Long userId = 4L;
        when(notificationService.getUserNotifications(userId)).thenReturn(mockNotifications);

        // Act
        ResponseEntity<List<NotificationDto>> response = notificationController.getUserNotifications(userId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).isEqualTo(mockNotifications);
        verify(notificationService).getUserNotifications(userId);
    }

    @Test
    public void testGetUserNotifications_EmptyList() {
        // Arrange
        Long userId = 7L;
        when(notificationService.getUserNotifications(userId)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<NotificationDto>> response = notificationController.getUserNotifications(userId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
        verify(notificationService).getUserNotifications(userId);
    }

    @Test
    public void testMarkAsRead_Success() {
        // Arrange
        Long notificationId = 1L;

        // Act
        ResponseEntity<Void> response = notificationController.markAsRead(notificationId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).markAsRead(notificationId);
    }
}