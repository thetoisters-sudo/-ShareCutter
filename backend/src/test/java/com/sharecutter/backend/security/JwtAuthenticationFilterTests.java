package com.sharecutter.backend.security;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.UserRole;
import com.sharecutter.backend.domain.enums.UserStatus;
import com.sharecutter.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    private static final String EMAIL =
            "user@example.com";

    private static final String ACCESS_TOKEN =
            "valid-access-token";

    private static final String REFRESH_TOKEN =
            "valid-refresh-token";

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserEntity user;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userRepository
                );

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutAuthorizationHeaderContinuesWithoutAuthentication()
            throws Exception {

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain)
                .doFilter(request, response);

        verifyNoInteractions(
                jwtService,
                userRepository
        );
    }

    @Test
    void requestWithNonBearerHeaderContinuesWithoutAuthentication()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Basic credentials"
        );

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain)
                .doFilter(request, response);

        verifyNoInteractions(
                jwtService,
                userRepository
        );
    }

    @Test
    void requestWithEmptyBearerTokenContinuesWithoutAuthentication()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer "
        );

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain)
                .doFilter(request, response);

        verifyNoInteractions(
                jwtService,
                userRepository
        );
    }

    @Test
    void validAccessTokenAuthenticatesActiveUser()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ACCESS_TOKEN
        );

        when(
                jwtService.isAccessToken(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.isTokenValid(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.extractEmail(ACCESS_TOKEN)
        ).thenReturn(EMAIL);

        when(
                userRepository
                        .findByEmailIgnoreCaseAndDeletedAtIsNull(
                                EMAIL
                        )
        ).thenReturn(Optional.of(user));

        when(user.getStatus())
                .thenReturn(UserStatus.ACTIVE);

        when(
                jwtService.isTokenValid(
                        ACCESS_TOKEN,
                        user
                )
        ).thenReturn(true);

        when(user.getRole())
                .thenReturn(UserRole.USER);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertTrue(
                authentication instanceof
                        UsernamePasswordAuthenticationToken
        );

        assertTrue(authentication.isAuthenticated());

        assertSame(
                user,
                authentication.getPrincipal()
        );

        assertNull(authentication.getCredentials());

        assertEquals(
                1,
                authentication.getAuthorities().size()
        );

        assertTrue(
                authentication
                        .getAuthorities()
                        .stream()
                        .anyMatch(
                                authority ->
                                        authority
                                                .getAuthority()
                                                .equals("ROLE_USER")
                        )
        );

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void refreshTokenDoesNotAuthenticateUser()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + REFRESH_TOKEN
        );

        when(
                jwtService.isAccessToken(REFRESH_TOKEN)
        ).thenReturn(false);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(
                jwtService,
                never()
        ).isTokenValid(REFRESH_TOKEN);

        verifyNoInteractions(userRepository);

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void invalidAccessTokenDoesNotAuthenticateUser()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ACCESS_TOKEN
        );

        when(
                jwtService.isAccessToken(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.isTokenValid(ACCESS_TOKEN)
        ).thenReturn(false);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(
                jwtService,
                never()
        ).extractEmail(ACCESS_TOKEN);

        verifyNoInteractions(userRepository);

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void tokenForUnknownUserDoesNotAuthenticateRequest()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ACCESS_TOKEN
        );

        when(
                jwtService.isAccessToken(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.isTokenValid(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.extractEmail(ACCESS_TOKEN)
        ).thenReturn(EMAIL);

        when(
                userRepository
                        .findByEmailIgnoreCaseAndDeletedAtIsNull(
                                EMAIL
                        )
        ).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void disabledUserDoesNotAuthenticateRequest()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ACCESS_TOKEN
        );

        when(
                jwtService.isAccessToken(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.isTokenValid(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.extractEmail(ACCESS_TOKEN)
        ).thenReturn(EMAIL);

        when(
                userRepository
                        .findByEmailIgnoreCaseAndDeletedAtIsNull(
                                EMAIL
                        )
        ).thenReturn(Optional.of(user));

        when(user.getStatus())
                .thenReturn(UserStatus.DISABLED);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(
                jwtService,
                never()
        ).isTokenValid(
                ACCESS_TOKEN,
                user
        );

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void lockedUserDoesNotAuthenticateRequest()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ACCESS_TOKEN
        );

        when(
                jwtService.isAccessToken(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.isTokenValid(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.extractEmail(ACCESS_TOKEN)
        ).thenReturn(EMAIL);

        when(
                userRepository
                        .findByEmailIgnoreCaseAndDeletedAtIsNull(
                                EMAIL
                        )
        ).thenReturn(Optional.of(user));

        when(user.getStatus())
                .thenReturn(UserStatus.LOCKED);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(
                jwtService,
                never()
        ).isTokenValid(
                ACCESS_TOKEN,
                user
        );

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void tokenThatDoesNotMatchUserDoesNotAuthenticateRequest()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ACCESS_TOKEN
        );

        when(
                jwtService.isAccessToken(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.isTokenValid(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.extractEmail(ACCESS_TOKEN)
        ).thenReturn(EMAIL);

        when(
                userRepository
                        .findByEmailIgnoreCaseAndDeletedAtIsNull(
                                EMAIL
                        )
        ).thenReturn(Optional.of(user));

        when(user.getStatus())
                .thenReturn(UserStatus.ACTIVE);

        when(
                jwtService.isTokenValid(
                        ACCESS_TOKEN,
                        user
                )
        ).thenReturn(false);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void existingAuthenticationIsNotReplaced()
            throws Exception {

        Authentication existingAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "existing-user",
                        null,
                        java.util.List.of()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(existingAuthentication);

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ACCESS_TOKEN
        );

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertSame(
                existingAuthentication,
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(
                jwtService,
                userRepository
        );

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void unexpectedTokenExceptionClearsAuthenticationAndContinues()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ACCESS_TOKEN
        );

        when(
                jwtService.isAccessToken(ACCESS_TOKEN)
        ).thenThrow(
                new IllegalArgumentException(
                        "Malformed token"
                )
        );

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(userRepository);

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void validAdminTokenCreatesAdminAuthority()
            throws Exception {

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ACCESS_TOKEN
        );

        when(
                jwtService.isAccessToken(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.isTokenValid(ACCESS_TOKEN)
        ).thenReturn(true);

        when(
                jwtService.extractEmail(ACCESS_TOKEN)
        ).thenReturn(EMAIL);

        when(
                userRepository
                        .findByEmailIgnoreCaseAndDeletedAtIsNull(
                                EMAIL
                        )
        ).thenReturn(Optional.of(user));

        when(user.getStatus())
                .thenReturn(UserStatus.ACTIVE);

        when(
                jwtService.isTokenValid(
                        ACCESS_TOKEN,
                        user
                )
        ).thenReturn(true);

        when(user.getRole())
                .thenReturn(UserRole.ADMIN);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertTrue(authentication.isAuthenticated());

        assertFalse(
                authentication
                        .getAuthorities()
                        .isEmpty()
        );

        assertTrue(
                authentication
                        .getAuthorities()
                        .stream()
                        .anyMatch(
                                authority ->
                                        authority
                                                .getAuthority()
                                                .equals("ROLE_ADMIN")
                        )
        );

        verify(filterChain)
                .doFilter(request, response);
    }
}