package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.CommentDetailsDto;
import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.entity.User;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.security.JwtUtil;
import com.swe574.group2.backend.service.CommentService;
import com.swe574.group2.backend.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "https://swe573-frontend-594781402587.us-central1.run.app"})
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PostService postService;
    private final CommentService commentService;

    @Autowired
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtUtil jwtUtil, PostService postService, CommentService commentService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.postService = postService;
        this.commentService = commentService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        // Check if the email is already registered
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email is already registered."));
        }

        // Check if the username is already taken
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username is already taken."));
        }

        // Encode the password and save the user
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }


    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        try {
            // Authenticate the user using their email and password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );

            // Retrieve the user from the database to get full details
            User authenticatedUser = userRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate the token using email and userId
            String token = jwtUtil.generateToken(authenticatedUser.getEmail(), authenticatedUser.getId());

            // Create a JSON response with the token
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            // Handle incorrect email or password
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            // Handle other exceptions
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/{userId}/posts")
    public ResponseEntity<List<PostListDto>> getUserPosts(@PathVariable Long userId) {
        List<PostListDto> userPosts = postService.getUserPosts(userId);
        return ResponseEntity.ok(userPosts);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String,String>> getUserName(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(Map.of("username", user.getUsername()));
    }

    @GetMapping("/{userId}/comments")
    public ResponseEntity<List<CommentDetailsDto>> getUserComments(@PathVariable Long userId) {
        List<CommentDetailsDto> userComments = commentService.getUserComments(userId);
        return ResponseEntity.ok(userComments);
    }

}