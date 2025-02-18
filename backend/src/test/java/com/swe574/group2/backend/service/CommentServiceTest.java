package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.CommentCreateDto;
import com.swe574.group2.backend.dto.CommentDetailsDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    private User mockUser;
    private Post mockPost;
    private Comment mockComment;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");

        mockPost = new Post();
        mockPost.setId(1L);
        mockPost.setTitle("Test Post");
        mockPost.setUser(mockUser);
        mockPost.setUpvotedBy(new HashSet<>());
        mockPost.setDownvotedBy(new HashSet<>());

        mockComment = new Comment();
        mockComment.setId(1L);
        mockComment.setContent("Test Comment");
        mockComment.setUser(mockUser);
        mockComment.setPost(mockPost);
        mockComment.setCreatedAt(LocalDateTime.now());
        mockComment.setUpdatedAt(LocalDateTime.now());
        mockComment.setUpvotedBy(new HashSet<>());
        mockComment.setDownvotedBy(new HashSet<>());
        mockComment.setReplies(new ArrayList<>());
    }

    @Test
    void testCreateComment() {
        CommentCreateDto dto = new CommentCreateDto();
        dto.setContent("New Comment");
        dto.setPostId(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.save(any(Comment.class))).thenReturn(mockComment);

        Map<String, Long> result = commentService.createComment(dto, "test@example.com");

        assertNotNull(result);
        assertEquals(1L, result.get("commentId"));
        verify(notificationService, times(1)).sendCommentNotification(mockUser.getId(), mockPost, mockComment);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void testGetCommentsForPost() {
        when(commentRepository.findByPostId(1L)).thenReturn(List.of(mockComment));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        List<CommentDetailsDto> result = commentService.getCommentsForPost(1L, "test@example.com");

        assertEquals(1, result.size());
        assertEquals("Test Comment", result.get(0).getContent());
        verify(commentRepository, times(1)).findByPostId(1L);
    }

    @Test
    void testUpvoteComment() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(mockComment));

        Map<String, Long> result = commentService.upvoteComment(1L, "test@example.com");

        assertNotNull(result);
        assertEquals(1L, result.get("commentId"));
        assertEquals(1, mockComment.getUpvotesCount());
        verify(notificationService, times(1)).sendUpvoteNotification(mockUser.getId(), mockComment);
    }

    @Test
    void testDownvoteComment() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(mockComment));

        Map<String, Long> result = commentService.downvoteComment(1L, "test@example.com");

        assertNotNull(result);
        assertEquals(1L, result.get("commentId"));
        assertEquals(1, mockComment.getDownvotesCount());
        verify(commentRepository, times(1)).save(mockComment);
    }

    @Test
    void testGetUserComments() {
        when(commentRepository.findByUserId(1L)).thenReturn(List.of(mockComment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        List<CommentDetailsDto> result = commentService.getUserComments(1L);

        assertEquals(1, result.size());
        assertEquals("Test Comment", result.get(0).getContent());
        verify(commentRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testCreateCommentWithParent() {
        CommentCreateDto dto = new CommentCreateDto();
        dto.setContent("Reply to Comment");
        dto.setPostId(1L);
        dto.setParentCommentId(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(mockComment));

        Comment reply = new Comment();
        reply.setId(2L);
        reply.setContent("Reply to Comment");
        reply.setUser(mockUser);
        reply.setParentComment(mockComment);

        when(commentRepository.save(any(Comment.class))).thenReturn(reply);

        Map<String, Long> result = commentService.createComment(dto, "test@example.com");

        assertNotNull(result);
        assertEquals(2L, result.get("commentId"));
    }
}
