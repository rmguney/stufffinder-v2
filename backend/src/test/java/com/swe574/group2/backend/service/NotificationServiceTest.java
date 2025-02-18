package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.NotificationRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.NotificationDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.Notification;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User mockUser;
    private Post mockPost;
    private Comment mockComment;
    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");

        mockPost = new Post();
        mockPost.setId(1L);
        mockPost.setTitle("Test Post");
        mockPost.setUpvotedBy(new HashSet<>());
        mockPost.setDownvotedBy(new HashSet<>());

        mockComment = new Comment();
        mockComment.setId(1L);
        mockComment.setContent("Test Comment");
        mockComment.setPost(mockPost);
        mockComment.setUpvotedBy(new HashSet<>());
        mockComment.setDownvotedBy(new HashSet<>());
        mockComment.setReplies(new ArrayList<>());
        mockComment.setUser(mockUser);
        mockComment.setCreatedAt(LocalDateTime.now());
        mockComment.setUpdatedAt(LocalDateTime.now());

        mockNotification = new Notification();
        mockNotification.setId(1L);
        mockNotification.setUser(mockUser);
        mockNotification.setPost(mockPost);
        mockNotification.setComment(mockComment);
        mockNotification.setMessage("Test Notification");
        mockNotification.setType(Notification.NotificationType.COMMENT);
        mockNotification.setRead(false);

    }

    @Test
    void testSendCommentNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        assertDoesNotThrow(() -> notificationService.sendCommentNotification(1L, mockPost, mockComment));

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testSendUpvoteNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        assertDoesNotThrow(() -> notificationService.sendUpvoteNotification(1L, mockComment));

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testSendBestAnswerNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        assertDoesNotThrow(() -> notificationService.sendBestAnswerNotification(1L, mockPost, mockComment));

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testGetUserNotifications() {
        when(notificationRepository.findByUserIdAndIsReadFalse(1L))
                .thenReturn(List.of(mockNotification));

        List<NotificationDto> notifications = notificationService.getUserNotifications(1L);

        assertEquals(1, notifications.size());
        assertEquals("Test Notification", notifications.get(0).getMessage());
        assertEquals(1L, notifications.get(0).getPostId());
        assertEquals(1L, notifications.get(0).getCommentId());

        verify(notificationRepository, times(1)).findByUserIdAndIsReadFalse(1L);
    }

    @Test
    void testMarkAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(mockNotification));

        assertDoesNotThrow(() -> notificationService.markAsRead(1L));

        assertTrue(mockNotification.isRead());
        verify(notificationRepository, times(1)).save(mockNotification);
        verify(notificationRepository, times(1)).findById(1L);
    }

    @Test
    void testMarkAsReadThrowsExceptionWhenNotificationNotFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(1L));

        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
