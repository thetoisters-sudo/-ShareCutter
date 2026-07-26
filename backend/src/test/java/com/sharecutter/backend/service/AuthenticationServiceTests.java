package com.sharecutter.backend.service;

import com.sharecutter.backend.config.JwtProperties;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.UserStatus;
import com.sharecutter.backend.dto.auth.LoginRequest;
import com.sharecutter.backend.dto.auth.TokenResponse;
import com.sharecutter.backend.exception.InactiveUserException;
import com.sharecutter.backend.exception.InvalidCredentialsException;
import com.sharecutter.backend.repository.UserRepository;
import com.sharecutter.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTests {

    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "Password123!";
    private static final String PASSWORD_HASH = "encoded-password";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 900L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository,
                passwordEncoder,
                jwtService,
                jwtProperties
        );
    }

    @Test
    void loginShouldReturnTokensWhenCredentialsAreValid() {
        UserEntity user = createActiveUser();
        LoginRequest request = new LoginRequest(
                EMAIL,
                RAW_PASSWORD
        );

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                RAW_PASSWORD,
                PASSWORD_HASH
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn(ACCESS_TOKEN);

        when(jwtService.generateRefreshToken(user))
                .thenReturn(REFRESH_TOKEN);

        when(jwtProperties.getAccessTokenExpiration())
                .thenReturn(
                        Duration.ofSeconds(
                                ACCESS_TOKEN_EXPIRATION_SECONDS
                        )
                );

        TokenResponse response =
                authenticationService.login(request);

        assertEquals(
                ACCESS_TOKEN,
                response.accessToken()
        );

        assertEquals(
                REFRESH_TOKEN,
                response.refreshToken()
        );

        assertEquals(
                "Bearer",
                response.tokenType()
        );

        assertEquals(
                ACCESS_TOKEN_EXPIRATION_SECONDS,
                response.expiresIn()
        );

        verify(userRepository)
                .findByEmailIgnoreCase(EMAIL);

        verify(passwordEncoder)
                .matches(
                        RAW_PASSWORD,
                        PASSWORD_HASH
                );

        verify(jwtService)
                .generateAccessToken(user);

        verify(jwtService)
                .generateRefreshToken(user);
    }

    @Test
    void loginShouldNormalizeEmailBeforeSearching() {
        UserEntity user = createActiveUser();
        LoginRequest request = new LoginRequest(
                "  USER@EXAMPLE.COM  ",
                RAW_PASSWORD
        );

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                RAW_PASSWORD,
                PASSWORD_HASH
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn(ACCESS_TOKEN);

        when(jwtService.generateRefreshToken(user))
                .thenReturn(REFRESH_TOKEN);

        when(jwtProperties.getAccessTokenExpiration())
                .thenReturn(
                        Duration.ofSeconds(
                                ACCESS_TOKEN_EXPIRATION_SECONDS
                        )
                );

        authenticationService.login(request);

        verify(userRepository)
                .findByEmailIgnoreCase(EMAIL);
    }

    @Test
    void loginShouldThrowWhenUserDoesNotExist() {
        LoginRequest request = new LoginRequest(
                EMAIL,
                RAW_PASSWORD
        );

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(request)
        );

        verify(passwordEncoder, never())
                .matches(
                        RAW_PASSWORD,
                        PASSWORD_HASH
                );

        verify(jwtService, never())
                .generateAccessToken(
                        org.mockito.ArgumentMatchers.any()
                );

        verify(jwtService, never())
                .generateRefreshToken(
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void loginShouldThrowWhenPasswordIsInvalid() {
        UserEntity user = createActiveUser();
        LoginRequest request = new LoginRequest(
                EMAIL,
                RAW_PASSWORD
        );

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                RAW_PASSWORD,
                PASSWORD_HASH
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(request)
        );

        verify(jwtService, never())
                .generateAccessToken(user);

        verify(jwtService, never())
                .generateRefreshToken(user);
    }

    @Test
    void loginShouldThrowWhenUserIsDisabled() {
        UserEntity user = createActiveUser();
        user.setStatus(UserStatus.DISABLED);

        LoginRequest request = new LoginRequest(
                EMAIL,
                RAW_PASSWORD
        );

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                RAW_PASSWORD,
                PASSWORD_HASH
        )).thenReturn(true);

        InactiveUserException exception =
                assertThrows(
                        InactiveUserException.class,
                        () -> authenticationService.login(request)
                );

        assertSame(
                UserStatus.DISABLED,
                exception.getStatus()
        );

        verify(jwtService, never())
                .generateAccessToken(user);

        verify(jwtService, never())
                .generateRefreshToken(user);
    }

    @Test
    void loginShouldThrowWhenUserIsLocked() {
        UserEntity user = createActiveUser();
        user.setStatus(UserStatus.LOCKED);

        LoginRequest request = new LoginRequest(
                EMAIL,
                RAW_PASSWORD
        );

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                RAW_PASSWORD,
                PASSWORD_HASH
        )).thenReturn(true);

        InactiveUserException exception =
                assertThrows(
                        InactiveUserException.class,
                        () -> authenticationService.login(request)
                );

        assertSame(
                UserStatus.LOCKED,
                exception.getStatus()
        );

        verify(jwtService, never())
                .generateAccessToken(user);

        verify(jwtService, never())
                .generateRefreshToken(user);
    }

    private UserEntity createActiveUser() {
        return new UserEntity(
                EMAIL,
                PASSWORD_HASH,
                "Test",
                "User"
        );
    }
}