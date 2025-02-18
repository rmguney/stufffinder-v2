package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.CommentCreateDto;
import com.swe574.group2.backend.dto.CommentDetailsDto;
import com.swe574.group2.backend.service.CommentService;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private UserDetails userDetails;
    private CommentCreateDto commentCreateDto;

    @BeforeEach
    public void setUp() {
        userDetails = User.withUsername("testuser")
                .password("password")
                .authorities("USER")
                .build();

        commentCreateDto = new CommentCreateDto();
        commentCreateDto.setPostId(1L);
        commentCreateDto.setContent("Test comment content");
    }

    @Test
    public void testCreateComment() {
        // Arrange
        Map<String, Long> expectedResponse = Map.of("commentId", 1L);
        when(commentService.createComment(any(CommentCreateDto.class), anyString()))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<Map<String, Long>> response = commentController.createComment(commentCreateDto, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("commentId", 1L);
        verify(commentService).createComment(commentCreateDto, "testuser");
    }

    @Test
    public void testGetCommentsForPost_AuthenticatedUser() {
        // Arrange
        Long postId = 1L;
        List<CommentDetailsDto> mockComments = Arrays.asList(
                new CommentDetailsDto(),
                new CommentDetailsDto()
        );
        when(commentService.getCommentsForPost(postId, "testuser")).thenReturn(mockComments);

        // Act
        ResponseEntity<List<CommentDetailsDto>> response = commentController.getCommentsForPost(postId, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(commentService).getCommentsForPost(postId, "testuser");
    }

    @Test
    public void testGetCommentsForPost_UnauthenticatedUser() {
        // Arrange
        Long postId = 1L;
        List<CommentDetailsDto> mockComments = Arrays.asList(
                new CommentDetailsDto(),
                new CommentDetailsDto()
        );
        when(commentService.getCommentsForPost(postId, null)).thenReturn(mockComments);

        // Act
        ResponseEntity<List<CommentDetailsDto>> response = commentController.getCommentsForPost(postId, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(commentService).getCommentsForPost(postId, null);
    }

    @Test
    public void testUpvoteComment() {
        // Arrange
        Long commentId = 1L;
        Map<String, Long> expectedResponse = Map.of("votes", 5L);
        when(commentService.upvoteComment(commentId, "testuser")).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Map<String, Long>> response = commentController.upvoteComment(commentId, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("votes", 5L);
        verify(commentService).upvoteComment(commentId, "testuser");
    }

    @Test
    public void testDownvoteComment() {
        // Arrange
        Long commentId = 1L;
        Map<String, Long> expectedResponse = Map.of("votes", 3L);
        when(commentService.downvoteComment(commentId, "testuser")).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Map<String, Long>> response = commentController.downvoteComment(commentId, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("votes", 3L);
        verify(commentService).downvoteComment(commentId, "testuser");
    }
}