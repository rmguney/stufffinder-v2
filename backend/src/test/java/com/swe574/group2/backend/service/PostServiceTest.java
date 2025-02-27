package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.MysteryObjectRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.PostCreationDto;
import com.swe574.group2.backend.dto.PostDetailsDto;
import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.MysteryObject;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MysteryObjectRepository mysteryObjectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PostService postService;

    private User mockUser;
    private Post mockPost;
    private MysteryObject mockMysteryObject;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setUsername("testuser");

        mockMysteryObject = new MysteryObject();
        mockMysteryObject.setId(1L);

        mockPost = new Post();
        mockPost.setId(1L);
        mockPost.setUser(mockUser);
        mockPost.setMysteryObject(mockMysteryObject);
        mockPost.setUpvotedBy(new HashSet<>());
        mockPost.setDownvotedBy(new HashSet<>());
        mockPost.setUpvotesCount(0);
        mockPost.setDownvotesCount(0);
    }

    @Test
    void createPost_ShouldSavePostAndMysteryObject() {
        // Arrange
        PostCreationDto postCreationDto = new PostCreationDto();
        postCreationDto.setTitle("Test Post");
        postCreationDto.setContent("Test Content");
        postCreationDto.setMysteryObject(mockMysteryObject);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(mysteryObjectRepository.save(any())).thenReturn(mockMysteryObject);
        when(postRepository.save(any())).thenReturn(mockPost);

        // Act
        Map<String, Long> result = postService.createPost(postCreationDto, "test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(mockPost.getId(), result.get("postId"));
        assertEquals(mockMysteryObject.getId(), result.get("mysteryObjectId"));

        verify(mysteryObjectRepository).save(any());
        verify(postRepository).save(any());
    }

    @Test
    void upvotePost_FirstUpvote_ShouldIncreaseUpvotes() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(mockPost));
        when(postRepository.save(any())).thenReturn(mockPost);

        // Act
        Map<String, Long> result = postService.upvotePost(1L, "test@example.com");

        // Assert
        assertEquals(1L, result.get("postId"));
        assertEquals(1, mockPost.getUpvotesCount());
        assertTrue(mockPost.getUpvotedBy().contains(mockUser));
    }

    @Test
    void upvotePost_RemoveUpvote_ShouldDecreaseUpvotes() {
        // Arrange
        mockPost.getUpvotedBy().add(mockUser);
        mockPost.setUpvotesCount(1);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(mockPost));
        when(postRepository.save(any())).thenReturn(mockPost);

        // Act
        Map<String, Long> result = postService.upvotePost(1L, "test@example.com");

        // Assert
        assertEquals(1L, result.get("postId"));
        assertEquals(0, mockPost.getUpvotesCount());
        assertFalse(mockPost.getUpvotedBy().contains(mockUser));
    }

    @Test
    void downvotePost_FirstDownvote_ShouldIncreaseDownvotes() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(mockPost));
        when(postRepository.save(any())).thenReturn(mockPost);

        // Act
        Map<String, Long> result = postService.downvotePost(1L, "test@example.com");

        // Assert
        assertEquals(1L, result.get("postId"));
        assertEquals(1, mockPost.getDownvotesCount());
        assertTrue(mockPost.getDownvotedBy().contains(mockUser));
    }

    /* @Test
    void getPostDetails_WithAuthenticatedUser_ShouldReturnPostDetails() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(postRepository.findPostDetailsById(anyLong())).thenReturn(mockPost);
        when(postRepository.findTagsByPostId(anyLong())).thenReturn(new HashSet<>());

        // Act
        PostDetailsDto result = postService.getPostDetails(1L, "test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(mockPost.getId(), result.getId());
        assertFalse(result.isUserUpvoted());
        assertFalse(result.isUserDownvoted());
    } */

    @Test
    void markBestAnswer_AsPostAuthor_ShouldMarkAnswerAndSendNotification() {
        // Arrange
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUser(new User());

        when(postRepository.findById(anyLong())).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any())).thenReturn(comment);
        when(postRepository.save(any())).thenReturn(mockPost);

        // Act
        boolean result = postService.markBestAnswer(1L, 1L, "testuser");

        // Assert
        assertTrue(result);
        assertTrue(mockPost.isSolved());
        assertTrue(comment.isBestAnswer());
    }

    /* @Test
    void searchPosts_ShouldReturnMatchingPosts() {
        // Arrange
        Post post = new Post();
        post.setId(1L); // Explicitly set the post ID
        post.setMysteryObject(new MysteryObject());
        Page<Post> postPage = new PageImpl<>(Collections.singletonList(post));

        when(postRepository.searchPosts(anyString(), any())).thenReturn(postPage);

        // Use the actual post ID when stubbing findTagsByPostId
        when(postRepository.findTagsByPostId(post.getId())).thenReturn(new HashSet<>());

        // Act
        Page<PostListDto> result = postService.searchPosts("keyword", PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        // Verify the method was called with the correct post ID
        verify(postRepository).findTagsByPostId(post.getId());
    }

    @Test
    void getUserPosts_ShouldReturnUserPosts() {
        // Arrange
        List<Post> posts = Collections.singletonList(mockPost);

        when(postRepository.findByUserId(anyLong())).thenReturn(posts);
        //when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));
        when(postRepository.findTagsByPostId(anyLong())).thenReturn(new HashSet<>());

        // Act
        List<PostListDto> result = postService.getUserPosts(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    } */
}