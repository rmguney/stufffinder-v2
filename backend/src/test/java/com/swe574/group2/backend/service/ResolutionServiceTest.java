package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.ResolutionRepository;
import com.swe574.group2.backend.dao.MediaFileRepository;
import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.ResolutionCreateDto;
import com.swe574.group2.backend.dto.ResolutionDetailsDto;
import com.swe574.group2.backend.entity.Resolution;
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
class ResolutionServiceTest {

    @Mock
    private ResolutionRepository resolutionRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private MediaFileRepository mediaFileRepository;

    @InjectMocks
    private ResolutionService resolutionService;

    private User mockUser;
    private Post mockPost;
    private Comment mockComment;
    private Resolution mockResolution;

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
        mockPost.setSolved(true);

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
        mockComment.setSolving(true);

        mockResolution = new Resolution();
        mockResolution.setId(1L);
        mockResolution.setDescription("Test Resolution");
        mockResolution.setUser(mockUser);
        mockResolution.setPost(mockPost);
        mockResolution.setCreatedAt(LocalDateTime.now());
        mockResolution.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateResolution() {
        ResolutionCreateDto dto = new ResolutionCreateDto();
        dto.setDescription("New Resolution");
        dto.setPostId(1L);
        dto.setComments(List.of(1L));

        // Mock repository responses
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.findAllById(List.of(1L))).thenReturn(List.of(mockComment)); // Mock comment retrieval
        when(resolutionRepository.save(any(Resolution.class))).thenReturn(mockResolution);

        Map<String, Long> result = resolutionService.createResolution(dto, "test@example.com");

        assertNotNull(result);
        assertEquals(1L, result.get("resolutionId"));
        // Verify interactions
        verify(resolutionRepository, times(1)).save(any(Resolution.class));
         
        verify(commentRepository, times(1)).saveAll(argThat(comments -> {
            List<Comment> commentList = new ArrayList<>();
            comments.forEach(commentList::add); // Convert Iterable to List
            return !commentList.isEmpty() && commentList.get(0).isSolving();
        }));// Ensure comment is updated

        verify(postRepository, times(1)).save(mockPost); // Ensure post is marked as solved
    }

    @Test
    void testGetResolutionsForPost() {
        when(resolutionRepository.findByPostId(1L)).thenReturn(List.of(mockResolution));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(mediaFileRepository.findByResolutionId(anyLong())).thenReturn(Collections.emptyList()); // Mock this

        List<ResolutionDetailsDto> result = resolutionService.getResolutionsForPost(1L, "test@example.com");

        assertEquals(1, result.size());
        assertEquals("Test Resolution", result.get(0).getDescription());
        verify(resolutionRepository, times(1)).findByPostId(1L);
    }
}
