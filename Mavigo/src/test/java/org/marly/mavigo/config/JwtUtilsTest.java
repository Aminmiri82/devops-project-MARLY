package org.marly.mavigo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secretKey = "testSecretKeyForJwtUtilsImplementationPlanApproved";
    private final long expirationTime = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtUtils, "expirationTime", expirationTime);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String email = "test@example.com";
        String token = jwtUtils.generateToken(email);

        assertNotNull(token);
        assertEquals(email, jwtUtils.extractUsername(token));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String email = "test@example.com";
        String token = jwtUtils.generateToken(email);
        UserDetails userDetails = new User(email, "password", Collections.emptyList());

        assertTrue(jwtUtils.validateToken(token, userDetails));
    }

    @Test
    void validateToken_shouldReturnFalseForMismatchedUser() {
        String email = "test@example.com";
        String token = jwtUtils.generateToken(email);
        UserDetails userDetails = new User("wrong@example.com", "password", Collections.emptyList());

        assertFalse(jwtUtils.validateToken(token, userDetails));
    }

    @Test
    void isTokenExpired_shouldReturnTrueForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtils, "expirationTime", -1000L); // Already expired
        String token = jwtUtils.generateToken("test@example.com");

        UserDetails userDetails = new User("test@example.com", "password", Collections.emptyList());
        
        // validateToken calls isTokenExpired
        assertThrows(Exception.class, () -> jwtUtils.validateToken(token, userDetails));
    }
}
