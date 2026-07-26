package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.enums.UserStatus;
import com.sharecutter.backend.dto.auth.LoginRequest;
import com.sharecutter.backend.dto.auth.TokenResponse;
import com.sharecutter.backend.exception.GlobalExceptionHandler;
import com.sharecutter.backend.exception.InactiveUserException;
import com.sharecutter.backend.exception.InvalidCredentialsException;
import com.sharecutter.backend.security.JwtAuthenticationFilter;
import com.sharecutter.backend.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthenticationControllerTests {

    private static final String LOGIN_PATH =
            "/api/v1/auth/login";

    private static final String EMAIL =
            "user@example.com";

    private static final String PASSWORD =
            "StrongPassword123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void loginReturnsTokenResponseWhenCredentialsAreValid()
            throws Exception {

        TokenResponse tokenResponse = new TokenResponse(
                "access-token",
                "refresh-token",
                900L
        );

        when(
                authenticationService.login(
                        any(LoginRequest.class)
                )
        ).thenReturn(tokenResponse);

        mockMvc.perform(
                        post(LOGIN_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "StrongPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .value("access-token")
                )
                .andExpect(
                        jsonPath("$.refreshToken")
                                .value("refresh-token")
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.expiresIn")
                                .value(900)
                );

        verify(authenticationService)
                .login(
                        new LoginRequest(
                                EMAIL,
                                PASSWORD
                        )
                );
    }

    @Test
    void loginRejectsInvalidEmail()
            throws Exception {

        mockMvc.perform(
                        post(LOGIN_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "not-an-email",
                                          "password": "StrongPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(LOGIN_PATH)
                );

        verifyNoInteractions(authenticationService);
    }

    @Test
    void loginRejectsShortPassword()
            throws Exception {

        mockMvc.perform(
                        post(LOGIN_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "short"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(LOGIN_PATH)
                );

        verifyNoInteractions(authenticationService);
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials()
            throws Exception {

        InvalidCredentialsException exception =
                mock(InvalidCredentialsException.class);

        when(exception.getMessage())
                .thenReturn("Invalid email or password");

        when(
                authenticationService.login(
                        any(LoginRequest.class)
                )
        ).thenThrow(exception);

        mockMvc.perform(
                        post(LOGIN_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "StrongPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(401)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid email or password"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(LOGIN_PATH)
                );
    }

    @Test
    void loginReturnsForbiddenForDisabledUser()
            throws Exception {

        InactiveUserException exception =
                mock(InactiveUserException.class);

        when(exception.getMessage())
                .thenReturn(
                        "User account is not active: DISABLED"
                );

        when(exception.getStatus())
                .thenReturn(UserStatus.DISABLED);

        when(
                authenticationService.login(
                        any(LoginRequest.class)
                )
        ).thenThrow(exception);

        mockMvc.perform(
                        post(LOGIN_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "StrongPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(403)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Forbidden")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "User account is not active: " +
                                                "DISABLED"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(LOGIN_PATH)
                );
    }

    @Test
    void loginReturnsForbiddenForLockedUser()
            throws Exception {

        InactiveUserException exception =
                mock(InactiveUserException.class);

        when(exception.getMessage())
                .thenReturn(
                        "User account is not active: LOCKED"
                );

        when(exception.getStatus())
                .thenReturn(UserStatus.LOCKED);

        when(
                authenticationService.login(
                        any(LoginRequest.class)
                )
        ).thenThrow(exception);

        mockMvc.perform(
                        post(LOGIN_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "StrongPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(403)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Forbidden")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "User account is not active: " +
                                                "LOCKED"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(LOGIN_PATH)
                );
    }

    @Test
    void loginReturnsInternalServerErrorForUnexpectedException()
            throws Exception {

        when(
                authenticationService.login(
                        any(LoginRequest.class)
                )
        ).thenThrow(
                new IllegalStateException(
                        "Token generation failed"
                )
        );

        mockMvc.perform(
                        post(LOGIN_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "StrongPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isInternalServerError()
                )
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(500)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Internal Server Error"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An unexpected error occurred"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(LOGIN_PATH)
                );
    }
}