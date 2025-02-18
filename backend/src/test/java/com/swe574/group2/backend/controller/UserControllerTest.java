package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.CommentDetailsDto;
import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.entity.User;
import com.swe574.group2.backend.security.JwtUtil;
import com.swe574.group2.backend.service.CommentService;
import com.swe574.group2.backend.service.PostService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword("rawpassword");
    }

    @Test
    public void testRegisterUser_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedpassword");

        // Act
        ResponseEntity<?> response = userController.registerUser(testUser);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isInstanceOf(Map.class)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("message", "User registered successfully!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testRegisterUser_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act
        ResponseEntity<?> response = userController.registerUser(testUser);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .isInstanceOf(Map.class)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Email is already registered.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testRegisterUser_UsernameAlreadyTaken() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act
        ResponseEntity<?> response = userController.registerUser(testUser);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .isInstanceOf(Map.class)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Username is already taken.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testLoginUser_Success() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("mockedToken");

        // Act
        ResponseEntity<?> response = userController.loginUser(testUser);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isInstanceOf(Map.class)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("token", "mockedToken");
    }

    @Test
    public void testLoginUser_BadCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act
        ResponseEntity<?> response = userController.loginUser(testUser);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
                .isInstanceOf(Map.class)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("error", "Invalid email or password");
    }

    @Test
    public void testGetUserPosts() {
        // Arrange
        Long userId = 1L;
        List<PostListDto> mockPosts = Arrays.asList(
                new PostListDto(),
                new PostListDto()
        );
        when(postService.getUserPosts(userId)).thenReturn(mockPosts);

        // Act
        ResponseEntity<List<PostListDto>> response = userController.getUserPosts(userId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(postService).getUserPosts(userId);
    }

    @Test
    public void testGetUserName() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<Map<String,String>> response = userController.getUserName(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("username", "testuser");
    }

    @Test
    public void testGetUserComments() {
        // Arrange
        Long userId = 1L;
        List<CommentDetailsDto> mockComments = Arrays.asList(
                new CommentDetailsDto(),
                new CommentDetailsDto()
        );
        when(commentService.getUserComments(userId)).thenReturn(mockComments);

        // Act
        ResponseEntity<List<CommentDetailsDto>> response = userController.getUserComments(userId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(commentService).getUserComments(userId);
    }
}