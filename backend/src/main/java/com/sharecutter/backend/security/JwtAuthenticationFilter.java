package com.sharecutter.backend.security;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.UserStatus;
import com.sharecutter.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(
                BEARER_PREFIX.length()
        );

        authenticateRequest(token);

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(String token) {
        if (SecurityContextHolder
                .getContext()
                .getAuthentication() != null) {
            return;
        }

        try {
            if (!jwtService.isAccessToken(token)) {
                return;
            }

            if (!jwtService.isTokenValid(token)) {
                return;
            }

            String email =
                    jwtService.extractEmail(token);

            Optional<UserEntity> optionalUser =
                    userRepository
                            .findByEmailIgnoreCaseAndDeletedAtIsNull(
                                    email
                            );

            if (optionalUser.isEmpty()) {
                return;
            }

            UserEntity user = optionalUser.get();

            if (user.getStatus() != UserStatus.ACTIVE) {
                return;
            }

            if (!jwtService.isTokenValid(token, user)) {
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

        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
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