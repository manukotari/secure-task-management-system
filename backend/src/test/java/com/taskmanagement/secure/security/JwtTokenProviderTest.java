package com.taskmanagement.secure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351";
    private final long expirationMs = 3600000; // 1 hour
    private final long refreshExpirationMs = 86400000; // 1 day

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs, refreshExpirationMs);
    }

    @Test
    @DisplayName("Should generate valid JWT token with claims and extract user details")
    void shouldGenerateAndValidateToken() {
        UserPrincipal principal = new UserPrincipal(
                100L,
                "john.doe@example.com",
                "John Doe",
                "encodedPassword",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = jwtTokenProvider.generateAccessToken(principal);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        String email = jwtTokenProvider.getEmailFromToken(token);
        assertEquals("john.doe@example.com", email);

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        assertEquals(100L, userId);
    }

    @Test
    @DisplayName("Should reject invalid or malformed tokens")
    void shouldRejectInvalidTokens() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Should reject expired tokens")
    void shouldRejectExpiredTokens() {
        // Create token provider with 0 ms expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secret, -1000, refreshExpirationMs);

        UserPrincipal principal = new UserPrincipal(
                100L,
                "expired@example.com",
                "Expired User",
                "pwd",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = shortLivedProvider.generateAccessToken(principal);
        assertFalse(shortLivedProvider.validateToken(token));
    }
}
