package com.sharecutter.backend.security;

import com.sharecutter.backend.config.JwtProperties;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtService jwtService;
    private UserEntity user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(
                "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
        );
        jwtProperties.setIssuer("sharecutter-backend");
        jwtProperties.setAccessTokenExpiration(Duration.ofMinutes(15));
        jwtProperties.setRefreshTokenExpiration(Duration.ofDays(30));

        jwtService = new JwtService(jwtProperties);

        userId = UUID.randomUUID();

        user = mock(UserEntity.class);

        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@sharecutter.com");
        when(user.getRole()).thenReturn(UserRole.USER);
    }

    @Test
    void generateAccessTokenShouldCreateValidAccessToken() {
        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isTokenValid(token, user));
        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
    }

    @Test
    void generateRefreshTokenShouldCreateValidRefreshToken() {
        String token = jwtService.generateRefreshToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isTokenValid(token, user));
        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isAccessToken(token));
    }

    @Test
    void extractEmailShouldReturnUserEmail() {
        String token = jwtService.generateAccessToken(user);

        String email = jwtService.extractEmail(token);

        assertEquals(user.getEmail(), email);
    }

    @Test
    void extractUserIdShouldReturnUserId() {
        String token = jwtService.generateAccessToken(user);

        UUID extractedUserId = jwtService.extractUserId(token);

        assertEquals(userId, extractedUserId);
    }

    @Test
    void extractRoleShouldReturnUserRole() {
        String token = jwtService.generateAccessToken(user);

        String role = jwtService.extractRole(token);

        assertEquals(UserRole.USER.name(), role);
    }

    @Test
    void tokenShouldBeInvalidForDifferentUser() {
        String token = jwtService.generateAccessToken(user);

        UserEntity differentUser = mock(UserEntity.class);

        when(differentUser.getId()).thenReturn(UUID.randomUUID());
        when(differentUser.getEmail())
                .thenReturn("different@sharecutter.com");
        when(differentUser.getRole()).thenReturn(UserRole.USER);

        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    void malformedTokenShouldBeInvalid() {
        assertFalse(jwtService.isTokenValid("not-a-valid-jwt"));
    }
}