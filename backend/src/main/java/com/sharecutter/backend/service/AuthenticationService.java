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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public TokenResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        UserEntity user = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(
                        normalizedEmail
                )
                .orElseThrow(InvalidCredentialsException::new);

        validatePassword(
                request.password(),
                user.getPasswordHash()
        );

        validateUserStatus(user.getStatus());

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken(user);

        long expiresIn = jwtProperties
                .getAccessTokenExpiration()
                .toSeconds();

        return new TokenResponse(
                accessToken,
                refreshToken,
                expiresIn
        );
    }

    private void validatePassword(
            String rawPassword,
            String passwordHash
    ) {
        if (!passwordEncoder.matches(
                rawPassword,
                passwordHash
        )) {
            throw new InvalidCredentialsException();
        }
    }

    private void validateUserStatus(UserStatus status) {
        if (status != UserStatus.ACTIVE) {
            throw new InactiveUserException(status);
        }
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase();
    }
}