package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.MediaFileRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.CommentCreateDto;
import com.swe574.group2.backend.dto.CommentDetailsDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import com.swe574.group2.backend.enums.CommentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private FollowerNotificationService followerNotificationService;

    @InjectMocks
    private CommentService commentService;

    private User user;
    private Post post;
    private Comment comment;
    private CommentCreateDto commentCreateDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");

        post = new Post();
        post.setId(1L);
        post.setUser(user); // Assuming Post has a User field for the author

        comment = new Comment();
        comment.setId(1L);
        comment.setUser(user);
        comment.setPost(post);
        comment.setContent("Test comment");
        comment.setCommentType(CommentType.QUESTION);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setUpvotedBy(new HashSet<>());
        comment.setDownvotedBy(new HashSet<>());
        comment.setReplies(new ArrayList<>());

        commentCreateDto = new CommentCreateDto();
        commentCreateDto.setPostId(post.getId());
        commentCreateDto.setContent("New comment content");
        commentCreateDto.setCommentType(CommentType.QUESTION); // Changed from ANSWER
    }

    @Test
    void createComment_success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment savedComment = invocation.getArgument(0);
            savedComment.setId(2L); // Simulate saving and getting an ID
            return savedComment;
        });

        Map<String, Long> response = commentService.createComment(commentCreateDto, user.getEmail());

        assertNotNull(response);
        assertEquals(2L, response.get("commentId"));
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(notificationService, times(1)).sendCommentNotification(anyLong(), any(Post.class), any(Comment.class));
        verify(followerNotificationService, times(1)).sendFollowedPostCommentNotification(any(Post.class), any(User.class), any(Comment.class));
    }
    
    @Test
    void createComment_withParentComment_success() {
        Comment parentComment = new Comment();
        parentComment.setId(3L);
        parentComment.setUser(new User()); // Different user for parent comment
        parentComment.getUser().setId(2L);


        commentCreateDto.setParentCommentId(parentComment.getId());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(post));
        when(commentRepository.findById(parentComment.getId())).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment savedComment = invocation.getArgument(0);
            savedComment.setId(4L); 
            return savedComment;
        });

        Map<String, Long> response = commentService.createComment(commentCreateDto, user.getEmail());

        assertNotNull(response);
        assertEquals(4L, response.get("commentId"));
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(notificationService, times(2)).sendCommentNotification(anyLong(), any(Post.class), any(Comment.class)); // Once for post author, once for parent comment author
        verify(followerNotificationService, times(1)).sendFollowedPostCommentNotification(any(Post.class), any(User.class), any(Comment.class));
    }


    @Test
    void createComment_userNotFound_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            commentService.createComment(commentCreateDto, "unknown@example.com");
        });
    }

    @Test
    void createComment_postNotFound_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(postRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            commentService.createComment(commentCreateDto, user.getEmail());
        });
    }
    
    @Test
    void createComment_parentCommentNotFound_throwsException() {
        commentCreateDto.setParentCommentId(999L); // Non-existent parent comment ID
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(post));
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            commentService.createComment(commentCreateDto, user.getEmail());
        });
    }

    @Test
    void getCommentsForPost_success() {
        Comment reply = new Comment();
        reply.setId(2L);
        reply.setUser(user);
        reply.setPost(post);
        reply.setContent("Reply comment");
        reply.setParentComment(comment); // This is a reply to 'comment'
        reply.setCommentType(CommentType.QUESTION); // Changed from ANSWER
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());
        reply.setUpvotedBy(new HashSet<>());
        reply.setDownvotedBy(new HashSet<>());
        reply.setReplies(new ArrayList<>());


        comment.getReplies().add(reply); // Add reply to the parent comment's replies list

        // getCommentsForPost should only return top-level comments
        List<Comment> commentsFromRepo = Arrays.asList(comment, reply); 
        when(commentRepository.findByPostId(anyLong())).thenReturn(commentsFromRepo);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(mediaFileRepository.findByCommentId(comment.getId())).thenReturn(new ArrayList<>());


        List<CommentDetailsDto> result = commentService.getCommentsForPost(post.getId(), user.getEmail());

        assertNotNull(result);
        assertEquals(1, result.size()); // Only the top-level comment should be returned
        assertEquals(comment.getId(), result.get(0).getId());
        assertEquals(1, result.get(0).getReplies().size()); // The reply should be nested
        verify(commentRepository, times(1)).findByPostId(post.getId());
    }
    
    @Test
    void getCommentsForPost_noUserEmail_success() {
        List<Comment> commentsFromRepo = Collections.singletonList(comment);
        when(commentRepository.findByPostId(anyLong())).thenReturn(commentsFromRepo);
        // userRepository.findByEmail should not be called if email is null
        when(mediaFileRepository.findByCommentId(comment.getId())).thenReturn(new ArrayList<>());


        List<CommentDetailsDto> result = commentService.getCommentsForPost(post.getId(), null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(comment.getId(), result.get(0).getId());
        assertFalse(result.get(0).isUserUpvoted()); // Should be false as no user context
        assertFalse(result.get(0).isUserDownvoted()); // Should be false as no user context
        verify(commentRepository, times(1)).findByPostId(post.getId());
        verify(userRepository, never()).findByEmail(anyString());
    }


    @Test
    void upvoteComment_newUserUpvote_success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Map<String, Long> response = commentService.upvoteComment(comment.getId(), user.getEmail());

        assertNotNull(response);
        assertEquals(comment.getId(), response.get("commentId"));
        assertTrue(comment.getUpvotedBy().contains(user));
        assertEquals(1, comment.getUpvotesCount());
        verify(commentRepository, times(1)).save(comment);
        verify(notificationService, times(1)).sendUpvoteNotification(anyLong(), any(Comment.class));
    }

    @Test
    void upvoteComment_removeExistingUpvote_success() {
        comment.getUpvotedBy().add(user);
        comment.setUpvotesCount(1);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Map<String, Long> response = commentService.upvoteComment(comment.getId(), user.getEmail());

        assertNotNull(response);
        assertEquals(comment.getId(), response.get("commentId"));
        assertFalse(comment.getUpvotedBy().contains(user));
        assertEquals(0, comment.getUpvotesCount());
        verify(commentRepository, times(1)).save(comment);
        // Notification might still be sent depending on logic, adjust if needed
        verify(notificationService, times(1)).sendUpvoteNotification(anyLong(), any(Comment.class));
    }
    
    @Test
    void upvoteComment_removeDownvoteAndAddUpvote_success() {
        comment.getDownvotedBy().add(user);
        comment.setDownvotesCount(1);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Map<String, Long> response = commentService.upvoteComment(comment.getId(), user.getEmail());

        assertNotNull(response);
        assertEquals(comment.getId(), response.get("commentId"));
        assertTrue(comment.getUpvotedBy().contains(user));
        assertEquals(1, comment.getUpvotesCount());
        assertFalse(comment.getDownvotedBy().contains(user));
        assertEquals(0, comment.getDownvotesCount());
        verify(commentRepository, times(1)).save(comment);
        verify(notificationService, times(1)).sendUpvoteNotification(anyLong(), any(Comment.class));
    }


    @Test
    void downvoteComment_newDownvote_success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Map<String, Long> response = commentService.downvoteComment(comment.getId(), user.getEmail());

        assertNotNull(response);
        assertEquals(comment.getId(), response.get("commentId"));
        assertTrue(comment.getDownvotedBy().contains(user));
        assertEquals(1, comment.getDownvotesCount());
        verify(commentRepository, times(1)).save(comment);
    }
    
    @Test
    void downvoteComment_removeExistingDownvote_success() {
        comment.getDownvotedBy().add(user);
        comment.setDownvotesCount(1);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Map<String, Long> response = commentService.downvoteComment(comment.getId(), user.getEmail());

        assertNotNull(response);
        assertEquals(comment.getId(), response.get("commentId"));
        assertFalse(comment.getDownvotedBy().contains(user));
        assertEquals(0, comment.getDownvotesCount());
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    void downvoteComment_removeUpvoteAndAddDownvote_success() {
        comment.getUpvotedBy().add(user);
        comment.setUpvotesCount(1);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Map<String, Long> response = commentService.downvoteComment(comment.getId(), user.getEmail());

        assertNotNull(response);
        assertEquals(comment.getId(), response.get("commentId"));
        assertTrue(comment.getDownvotedBy().contains(user));
        assertEquals(1, comment.getDownvotesCount());
        assertFalse(comment.getUpvotedBy().contains(user));
        assertEquals(0, comment.getUpvotesCount());
        verify(commentRepository, times(1)).save(comment);
    }


    @Test
    void getUserComments_success() {
        List<Comment> userComments = Collections.singletonList(comment);
        when(commentRepository.findByUserId(anyLong())).thenReturn(userComments);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(mediaFileRepository.findByCommentId(comment.getId())).thenReturn(new ArrayList<>());


        List<CommentDetailsDto> result = commentService.getUserComments(user.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(comment.getContent(), result.get(0).getContent());
        verify(commentRepository, times(1)).findByUserId(user.getId());
        verify(userRepository, times(1)).findById(user.getId());
    }
    
    @Test
    void getUserComments_userNotFound_throwsException() {
        when(commentRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList()); // Or some comments
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            commentService.getUserComments(999L); // Non-existent user ID
        });
    }


    @Test
    void editComment_success() {
        CommentCreateDto editDto = new CommentCreateDto();
        editDto.setContent("Updated content");
        editDto.setCommentType(CommentType.QUESTION); // Changed from CLARIFICATION

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Long> response = commentService.editComment(comment.getId(), editDto, user.getEmail());

        assertNotNull(response);
        assertEquals(comment.getId(), response.get("commentId"));
        assertEquals("Updated content", comment.getContent());
        assertEquals(CommentType.QUESTION, comment.getCommentType()); // Changed from CLARIFICATION
        assertNotNull(comment.getUpdatedAt());
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    void editComment_notAuthor_throwsForbiddenException() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("another@example.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(anotherUser));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(comment)); // comment belongs to 'user'

        CommentCreateDto editDto = new CommentCreateDto();
        editDto.setContent("Attempt to edit");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            commentService.editComment(comment.getId(), editDto, anotherUser.getEmail());
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode()); // Changed from getStatus()
    }
    
    @Test
    void editComment_commentNotFound_throwsException() {
        CommentCreateDto editDto = new CommentCreateDto();
        editDto.setContent("Updated content");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            commentService.editComment(999L, editDto, user.getEmail());
        });
    }
    
    @Test
    void editComment_onlyContentUpdate_success() {
        CommentCreateDto editDto = new CommentCreateDto();
        editDto.setContent("Only content updated");
        // commentCreateDto.setCommentType is null

        Comment originalComment = new Comment();
        originalComment.setId(1L);
        originalComment.setUser(user);
        originalComment.setPost(post);
        originalComment.setContent("Original content");
        originalComment.setCommentType(CommentType.QUESTION); // Original type
        originalComment.setCreatedAt(LocalDateTime.now().minusDays(1));
        originalComment.setUpdatedAt(LocalDateTime.now().minusDays(1));


        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(originalComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Long> response = commentService.editComment(originalComment.getId(), editDto, user.getEmail());

        assertNotNull(response);
        assertEquals(originalComment.getId(), response.get("commentId"));
        assertEquals("Only content updated", originalComment.getContent());
        assertEquals(CommentType.QUESTION, originalComment.getCommentType()); // Type should remain unchanged
        assertNotNull(originalComment.getUpdatedAt());
        assertTrue(originalComment.getUpdatedAt().isAfter(originalComment.getCreatedAt()));
        verify(commentRepository, times(1)).save(originalComment);
    }
}
