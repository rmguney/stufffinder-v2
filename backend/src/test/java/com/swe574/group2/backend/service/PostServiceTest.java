package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.*;
import com.swe574.group2.backend.service.FollowerNotificationService;
import com.swe574.group2.backend.service.NotificationService;
import com.swe574.group2.backend.dto.PostCreationDto;
import com.swe574.group2.backend.dto.PostDetailsDto;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import com.swe574.group2.backend.entity.MysteryObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private MysteryObjectRepository mysteryObjectRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private NotificationService notificationService;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private FollowerNotificationService followerNotificationService;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private PostService postService;

    private Post testPost;
    private User testUser;
    private PostCreationDto testPostCreationDto;
    private PostDetailsDto testPostDetailsDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");

        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setDescription("Test Description");
        testPost.setUser(testUser);
        testPost.setUpvotedBy(new HashSet<>());
        testPost.setDownvotedBy(new HashSet<>());

        testPostCreationDto = new PostCreationDto();
        testPostCreationDto.setTitle("Test Post");
        testPostCreationDto.setContent("Test Description");
        MysteryObject mysteryObject = new MysteryObject();
        mysteryObject.setSubParts(new ArrayList<>());
        testPostCreationDto.setMysteryObject(mysteryObject);
        testPostCreationDto.setTags(new HashSet<>());


        testPostDetailsDto = new PostDetailsDto();
        testPostDetailsDto.setId(1L);
        testPostDetailsDto.setTitle("Test Post");
        testPostDetailsDto.setDescription("Test Description");
    }

    @Test
    void whenCreatePost_thenReturnPostDetails() {
        // Given
        when(userRepository.findByEmail(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        when(mysteryObjectRepository.save(any(MysteryObject.class))).thenReturn(testPostCreationDto.getMysteryObject());
        testPost.setTagMap(new HashMap<>()); // Ensure tagMap is initialized

        // When
        Map<String, Long> result = postService.createPost(testPostCreationDto, testUser.getUsername());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("postId")).isNotNull();
    }

    @Test
    void whenGetPostDetails_thenReturnPostDetails() {
        // Given
        when(postRepository.findPostDetailsById(1L)).thenReturn(testPost);
        when(postRepository.findTagKeysByPostId(1L)).thenReturn(new HashSet<>());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        PostDetailsDto result = postService.getPostDetails(1L, testUser.getEmail());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testPost.getId());
        assertThat(result.getTitle()).isEqualTo(testPost.getTitle());
        assertThat(result.getDescription()).isEqualTo(testPost.getDescription());
    }
}
