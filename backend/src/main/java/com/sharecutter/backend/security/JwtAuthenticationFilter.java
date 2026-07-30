package com.sharecutter.backend.security;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.UserStatus;
import com.sharecutter.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    JwtAuthenticationFilter.class
            );

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (!hasBearerToken(authorizationHeader)) {
            LOGGER.debug(
                    "No bearer token supplied for {} {}",
                    request.getMethod(),
                    request.getRequestURI()
            );

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token = authorizationHeader.substring(
                BEARER_PREFIX.length()
        );

        authenticateRequest(
                token,
                request
        );

        filterChain.doFilter(
                request,
                response
        );
    }

    private void authenticateRequest(
            String token,
            HttpServletRequest request
    ) {
        if (
                SecurityContextHolder
                        .getContext()
                        .getAuthentication() != null
        ) {
            LOGGER.debug(
                    "Security context already contains authentication for {} {}",
                    request.getMethod(),
                    request.getRequestURI()
            );

            return;
        }

        try {
            LOGGER.info(
                    "Validating JWT for {} {}",
                    request.getMethod(),
                    request.getRequestURI()
            );

            if (!jwtService.isAccessToken(token)) {
                LOGGER.warn(
                        "JWT rejected because token type is not access for {} {}",
                        request.getMethod(),
                        request.getRequestURI()
                );

                return;
            }

            if (!jwtService.isTokenValid(token)) {
                LOGGER.warn(
                        "JWT rejected because signature, issuer or expiration is invalid for {} {}",
                        request.getMethod(),
                        request.getRequestURI()
                );

                return;
            }

            String email =
                    jwtService.extractEmail(token);

            LOGGER.info(
                    "JWT subject extracted successfully: {}",
                    email
            );

            Optional<UserEntity> optionalUser =
                    userRepository
                            .findByEmailIgnoreCaseAndDeletedAtIsNull(
                                    email
                            );

            if (optionalUser.isEmpty()) {
                LOGGER.warn(
                        "JWT user was not found in the database: {}",
                        email
                );

                return;
            }

            UserEntity user = optionalUser.get();

            LOGGER.info(
                    "JWT user loaded: id={}, email={}, role={}, status={}",
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus()
            );

            if (user.getStatus() != UserStatus.ACTIVE) {
                LOGGER.warn(
                        "JWT user is not active: email={}, status={}",
                        user.getEmail(),
                        user.getStatus()
                );

                return;
            }

            if (!jwtService.isTokenValid(token, user)) {
                LOGGER.warn(
                        "JWT subject does not match database user or token is expired: {}",
                        user.getEmail()
                );

                return;
            }

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            ROLE_PREFIX
                                    + user.getRole().name()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            LOGGER.info(
                    "JWT authentication completed successfully: email={}, authority={}",
                    user.getEmail(),
                    authority.getAuthority()
            );

        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();

            LOGGER.error(
                    "JWT authentication failed for {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getMessage(),
                    exception
            );
        }
    }

    private boolean hasBearerToken(
            String authorizationHeader
    ) {
        return authorizationHeader != null
                && authorizationHeader.startsWith(
                        BEARER_PREFIX
                )
                && authorizationHeader.length()
                > BEARER_PREFIX.length();
    }
}