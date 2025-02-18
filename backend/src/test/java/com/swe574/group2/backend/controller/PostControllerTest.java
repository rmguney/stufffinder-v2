package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.PostCreationDto;
import com.swe574.group2.backend.dto.PostDetailsDto;
import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private UserDetails userDetails;
    private PostCreationDto postCreationDto;

    @BeforeEach
    public void setUp() {
        userDetails = User.withUsername("testuser")
                .password("password")
                .authorities("USER")
                .build();

        postCreationDto = new PostCreationDto();
        postCreationDto.setTitle("Test Post");
    }

    @Test
    public void testCreatePost() throws IOException {
        // Arrange
        Map<String, Long> expectedResponse = Map.of("postId", 1L);
        when(postService.createPost(any(PostCreationDto.class), anyString()))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<Map<String, Long>> response = postController.createPost(postCreationDto, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("postId", 1L);
        verify(postService).createPost(postCreationDto, "testuser");
    }

    @Test
    public void testGetPostsForPostList() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostListDto> mockPage = new PageImpl<>(Collections.singletonList(new PostListDto()));
        when(postService.getAllPostsForPostList(pageable)).thenReturn(mockPage);

        // Act
        Page<PostListDto> result = postController.getPostsForPostList(pageable);

        // Assert
        assertThat(result).isEqualTo(mockPage);
        verify(postService).getAllPostsForPostList(pageable);
    }

    @Test
    public void testGetPostDetails() {
        // Arrange
        Long postId = 1L;
        PostDetailsDto mockPostDetails = new PostDetailsDto();
        when(postService.getPostDetails(postId, "testuser")).thenReturn(mockPostDetails);

        // Act
        ResponseEntity<PostDetailsDto> response = postController.getPostDetails(postId, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockPostDetails);
        verify(postService).getPostDetails(postId, "testuser");
    }

    @Test
    public void testGetPostDetails_UnauthenticatedUser() {
        // Arrange
        Long postId = 1L;
        PostDetailsDto mockPostDetails = new PostDetailsDto();
        when(postService.getPostDetails(postId, null)).thenReturn(mockPostDetails);

        // Act
        ResponseEntity<PostDetailsDto> response = postController.getPostDetails(postId, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockPostDetails);
        verify(postService).getPostDetails(postId, null);
    }

    @Test
    public void testUpvotePost() {
        // Arrange
        Long postId = 1L;
        Map<String, Long> expectedResponse = Map.of("votes", 5L);
        when(postService.upvotePost(postId, "testuser")).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Map<String, Long>> response = postController.upvotePost(postId, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("votes", 5L);
        verify(postService).upvotePost(postId, "testuser");
    }

    @Test
    public void testDownvotePost() {
        // Arrange
        Long postId = 1L;
        Map<String, Long> expectedResponse = Map.of("votes", 3L);
        when(postService.downvotePost(postId, "testuser")).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Map<String, Long>> response = postController.downvotePost(postId, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("votes", 3L);
        verify(postService).downvotePost(postId, "testuser");
    }

    @Test
    public void testMarkBestAnswer_Success() {
        // Arrange
        Long postId = 1L;
        Long commentId = 2L;
        when(postService.markBestAnswer(postId, commentId, "testuser")).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Long>> response = postController.markBestAnswer(postId, commentId, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("postId", postId)
                .containsEntry("commentId", commentId);
        verify(postService).markBestAnswer(postId, commentId, "testuser");
    }

    @Test
    public void testMarkBestAnswer_Failure() {
        // Arrange
        Long postId = 1L;
        Long commentId = 2L;
        when(postService.markBestAnswer(postId, commentId, "testuser")).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Long>> response = postController.markBestAnswer(postId, commentId, userDetails);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("postId", postId)
                .containsEntry("commentId", commentId);
        verify(postService).markBestAnswer(postId, commentId, "testuser");
    }

    @Test
    public void testSearchPosts() {
        // Arrange
        String query = "test query";
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostListDto> mockSearchResults = new PageImpl<>(Collections.singletonList(new PostListDto()));
        when(postService.searchPosts(query, pageable)).thenReturn(mockSearchResults);

        // Act
        ResponseEntity<Page<PostListDto>> response = postController.searchPosts(query, pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockSearchResults);
        verify(postService).searchPosts(query, pageable);
    }
}