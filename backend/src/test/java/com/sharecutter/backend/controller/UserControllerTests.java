package com.sharecutter.backend.controller;

import com.sharecutter.backend.config.SecurityConfig;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.UserRole;
import com.sharecutter.backend.domain.enums.UserStatus;
import com.sharecutter.backend.dto.user.UserResponse;
import com.sharecutter.backend.exception.GlobalExceptionHandler;
import com.sharecutter.backend.exception.UserAlreadyExistsException;
import com.sharecutter.backend.exception.UserNotFoundException;
import com.sharecutter.backend.mapper.UserMapper;
import com.sharecutter.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void createUserReturnsCreatedResponse() throws Exception {
        UUID userId = UUID.randomUUID();

        OffsetDateTime createdAt = OffsetDateTime.of(
                2026,
                7,
                25,
                19,
                0,
                0,
                0,
                ZoneOffset.UTC
        );

        UserEntity userEntity = mock(UserEntity.class);

        UserResponse response = new UserResponse(
                userId,
                "user@example.com",
                "Omri",
                "Cohen",
                UserRole.USER,
                UserStatus.ACTIVE,
                createdAt,
                createdAt
        );

        when(
                userService.createUser(
                        "user@example.com",
                        "StrongPassword123",
                        "Omri",
                        "Cohen"
                )
        ).thenReturn(userEntity);

        when(
                userMapper.toResponse(userEntity)
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "StrongPassword123",
                                          "firstName": "Omri",
                                          "lastName": "Cohen"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                "/api/v1/users/" + userId
                        )
                )
                .andExpect(
                        jsonPath("$.id").value(userId.toString())
                )
                .andExpect(
                        jsonPath("$.email").value(
                                "user@example.com"
                        )
                )
                .andExpect(
                        jsonPath("$.firstName").value("Omri")
                )
                .andExpect(
                        jsonPath("$.lastName").value("Cohen")
                )
                .andExpect(
                        jsonPath("$.role").value("USER")
                )
                .andExpect(
                        jsonPath("$.status").value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.passwordHash").doesNotExist()
                );
    }

    @Test
    void createUserRejectsInvalidEmail() throws Exception {
        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "not-an-email",
                                          "password": "StrongPassword123",
                                          "firstName": "Omri",
                                          "lastName": "Cohen"
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
                        jsonPath("$.error").value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.path").value("/api/v1/users")
                );

        verifyNoInteractions(
                userService,
                userMapper
        );
    }

    @Test
    void createUserRejectsShortPassword() throws Exception {
        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "short",
                                          "firstName": "Omri",
                                          "lastName": "Cohen"
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
                        jsonPath("$.error").value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.path").value("/api/v1/users")
                );

        verifyNoInteractions(
                userService,
                userMapper
        );
    }

    @Test
    void getUserByIdReturnsUserResponse() throws Exception {
        UUID userId = UUID.randomUUID();

        OffsetDateTime createdAt = OffsetDateTime.of(
                2026,
                7,
                25,
                19,
                0,
                0,
                0,
                ZoneOffset.UTC
        );

        UserEntity userEntity = mock(UserEntity.class);

        UserResponse response = new UserResponse(
                userId,
                "user@example.com",
                "Omri",
                "Cohen",
                UserRole.USER,
                UserStatus.ACTIVE,
                createdAt,
                createdAt
        );

        when(
                userService.getUserById(userId)
        ).thenReturn(userEntity);

        when(
                userMapper.toResponse(userEntity)
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/users/{userId}", userId)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id").value(userId.toString())
                )
                .andExpect(
                        jsonPath("$.email").value(
                                "user@example.com"
                        )
                )
                .andExpect(
                        jsonPath("$.firstName").value("Omri")
                )
                .andExpect(
                        jsonPath("$.lastName").value("Cohen")
                )
                .andExpect(
                        jsonPath("$.role").value("USER")
                )
                .andExpect(
                        jsonPath("$.status").value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.passwordHash").doesNotExist()
                );
    }

    @Test
    void getUserByIdReturnsNotFoundWhenUserDoesNotExist()
            throws Exception {

        UUID userId = UUID.randomUUID();

        UserNotFoundException exception =
                mock(UserNotFoundException.class);

        when(exception.getMessage())
                .thenReturn("User not found: " + userId);

        when(userService.getUserById(userId))
                .thenThrow(exception);

        mockMvc.perform(
                        get("/api/v1/users/{userId}", userId)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(404)
                )
                .andExpect(
                        jsonPath("$.error").value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message").value(
                                "User not found: " + userId
                        )
                )
                .andExpect(
                        jsonPath("$.path").value(
                                "/api/v1/users/" + userId
                        )
                );

        verifyNoInteractions(userMapper);
    }

    @Test
    void createUserReturnsConflictWhenEmailAlreadyExists()
            throws Exception {

        UserAlreadyExistsException exception =
                mock(UserAlreadyExistsException.class);

        when(exception.getMessage())
                .thenReturn(
                        "User already exists with email: " +
                                "user@example.com"
                );

        when(
                userService.createUser(
                        "user@example.com",
                        "StrongPassword123",
                        "Omri",
                        "Cohen"
                )
        ).thenThrow(exception);

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "StrongPassword123",
                                          "firstName": "Omri",
                                          "lastName": "Cohen"
                                        }
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(409)
                )
                .andExpect(
                        jsonPath("$.error").value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message").value(
                                "User already exists with email: " +
                                        "user@example.com"
                        )
                )
                .andExpect(
                        jsonPath("$.path").value("/api/v1/users")
                );

        verifyNoInteractions(userMapper);
    }

    @Test
    void createUserReturnsInternalServerErrorForUnexpectedException()
            throws Exception {

        when(
                userService.createUser(
                        "user@example.com",
                        "StrongPassword123",
                        "Omri",
                        "Cohen"
                )
        ).thenThrow(
                new IllegalStateException(
                        "Database connection failed"
                )
        );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "user@example.com",
                                          "password": "StrongPassword123",
                                          "firstName": "Omri",
                                          "lastName": "Cohen"
                                        }
                                        """
                                )
                )
                .andExpect(status().isInternalServerError())
                .andExpect(
                        jsonPath("$.timestamp").exists()
                )
                .andExpect(
                        jsonPath("$.status").value(500)
                )
                .andExpect(
                        jsonPath("$.error").value(
                                "Internal Server Error"
                        )
                )
                .andExpect(
                        jsonPath("$.message").value(
                                "An unexpected error occurred"
                        )
                )
                .andExpect(
                        jsonPath("$.path").value("/api/v1/users")
                );

        verifyNoInteractions(userMapper);
    }
}