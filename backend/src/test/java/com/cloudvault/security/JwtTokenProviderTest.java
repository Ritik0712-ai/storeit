package com.cloudvault.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        // Set required properties via reflection
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "test-secret-key-that-is-at-least-256-bits-long-for-testing");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiry", 900000L);
        tokenProvider.init();
    }

    @Test
    void shouldGenerateValidAccessToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        String token = tokenProvider.generateAccessToken(userId, email);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void shouldExtractUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        String token = tokenProvider.generateAccessToken(userId, email);
        UUID extractedId = tokenProvider.getUserIdFromToken(token);

        assertEquals(userId, extractedId);
    }

    @Test
    void shouldValidateValidToken() {
        UUID userId = UUID.randomUUID();

        String token = tokenProvider.generateAccessToken(userId, "test@example.com");
        boolean isValid = tokenProvider.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void shouldRejectInvalidToken() {
        boolean isValid = tokenProvider.validateToken("invalid.token.here");

        assertFalse(isValid);
    }

    @Test
    void shouldRejectEmptyToken() {
        boolean isValid = tokenProvider.validateToken("");

        assertFalse(isValid);
    }
}
