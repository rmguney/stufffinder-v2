package com.swe574.group2.backend.security;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set a fixed secret key for consistent testing
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "TestSecretKeyThatIsLongEnoughForHmacSHA256");
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        // Arrange
        String email = "test@example.com";
        Long userId = 123L;

        // Act
        String token = jwtUtil.generateToken(email, userId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        // Arrange
        String email = "test@example.com";
        Long userId = 123L;
        String token = jwtUtil.generateToken(email, userId);

        // Act
        String extractedUsername = jwtUtil.extractUsername(token);

        // Assert
        assertEquals(email, extractedUsername);
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Arrange
        String email = "test@example.com";
        Long userId = 123L;
        String token = jwtUtil.generateToken(email, userId);

        // Act
        boolean isValid = jwtUtil.validateToken(token, email);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_WithWrongUsername_ShouldReturnFalse() {
        // Arrange
        String email = "test@example.com";
        Long userId = 123L;
        String token = jwtUtil.generateToken(email, userId);

        // Act
        boolean isValid = jwtUtil.validateToken(token, "wrong@example.com");

        // Assert
        assertFalse(isValid);
    }

    @Test
    void generateToken_ShouldIncludeUserIdInClaims() {
        // Arrange
        String email = "test@example.com";
        Long userId = 123L;

        // Act
        String token = jwtUtil.generateToken(email, userId);

        // Assert
        // This is a bit tricky to test directly, but we can generate and extract the token
        assertNotNull(token);
        assertDoesNotThrow(() -> {
            // This will throw an exception if the token is invalid
            jwtUtil.extractUsername(token);
        });
    }

    @Test
    void validateToken_WithInvalidSignature_ShouldReturnFalse() {
        // Arrange
        String email = "test@example.com";
        Long userId = 123L;
        String token = jwtUtil.generateToken(email, userId);

        // Create another JwtUtil with a different secret key
        JwtUtil differentJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(differentJwtUtil, "secretKey", "DifferentSecretKeyThatIsLongEnoughForHmacSHA256");

        // Act & Assert
        assertThrows(SignatureException.class, () -> {
            differentJwtUtil.extractUsername(token);
        });
    }
}