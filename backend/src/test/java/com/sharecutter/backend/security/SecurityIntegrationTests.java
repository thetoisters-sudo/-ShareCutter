package com.sharecutter.backend.security;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.UserRole;
import com.sharecutter.backend.domain.enums.UserStatus;
import com.sharecutter.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTests {

    private static final String USER_EMAIL =
            "security.integration@example.com";

    private static final String ADMIN_EMAIL =
            "security.admin@example.com";

    private static final String PASSWORD =
            "StrongPassword123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private UserEntity activeUser;
    private UserEntity activeAdmin;

    @BeforeEach
    void setUp() {
        activeUser = new UserEntity(
                USER_EMAIL,
                passwordEncoder.encode(PASSWORD),
                "Security",
                "User"
        );

        activeUser.setRole(UserRole.USER);
        activeUser.setStatus(UserStatus.ACTIVE);

        activeUser =
                userRepository.saveAndFlush(activeUser);

        activeAdmin = new UserEntity(
                ADMIN_EMAIL,
                passwordEncoder.encode(PASSWORD),
                "Security",
                "Admin"
        );

        activeAdmin.setRole(UserRole.ADMIN);
        activeAdmin.setStatus(UserStatus.ACTIVE);

        activeAdmin =
                userRepository.saveAndFlush(activeAdmin);
    }

    @Test
    void loginEndpointIsPublic()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "security.integration@example.com",
                                          "password": "StrongPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.refreshToken").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                );
    }

    @Test
    void createUserEndpointIsPublic()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "new.user@example.com",
                                          "password": "StrongPassword123",
                                          "firstName": "New",
                                          "lastName": "User"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.email")
                                .value("new.user@example.com")
                )
                .andExpect(
                        jsonPath("$.firstName")
                                .value("New")
                )
                .andExpect(
                        jsonPath("$.lastName")
                                .value("User")
                );
    }

    @Test
    void currentUserEndpointRejectsMissingToken()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/users/me")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCanAccessCurrentUserEndpoint()
            throws Exception {

        String accessToken =
                jwtService.generateAccessToken(activeUser);

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        activeUser
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(USER_EMAIL)
                )
                .andExpect(
                        jsonPath("$.firstName")
                                .value("Security")
                )
                .andExpect(
                        jsonPath("$.lastName")
                                .value("User")
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("USER")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    void adminCanAccessCurrentUserEndpoint()
            throws Exception {

        String accessToken =
                jwtService.generateAccessToken(activeAdmin);

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        activeAdmin
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(ADMIN_EMAIL)
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("ADMIN")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    void regularUserCanUpdateOwnProfile()
            throws Exception {

        String accessToken =
                jwtService.generateAccessToken(activeUser);

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "firstName": "Updated",
                                          "lastName": "User"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.email")
                                .value(USER_EMAIL)
                )
                .andExpect(
                        jsonPath("$.firstName")
                                .value("Updated")
                )
                .andExpect(
                        jsonPath("$.lastName")
                                .value("User")
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("USER")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    void updateCurrentUserRejectsMissingToken()
            throws Exception {

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "firstName": "Updated",
                                          "lastName": "User"
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedUserEndpointRejectsMissingToken()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}",
                                activeUser.getId()
                        )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedUserEndpointRejectsInvalidToken()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}",
                                activeUser.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer invalid-token"
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotAccessAdminUserEndpoint()
            throws Exception {

        String accessToken =
                jwtService.generateAccessToken(activeUser);

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}",
                                activeUser.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessUserEndpoint()
            throws Exception {

        String accessToken =
                jwtService.generateAccessToken(activeAdmin);

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}",
                                activeUser.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        activeUser
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(USER_EMAIL)
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("USER")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    void protectedUserEndpointRejectsRefreshToken()
            throws Exception {

        String refreshToken =
                jwtService.generateRefreshToken(activeAdmin);

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}",
                                activeUser.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + refreshToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledAdminCannotAccessProtectedEndpoint()
            throws Exception {

        activeAdmin.setStatus(UserStatus.DISABLED);

        activeAdmin =
                userRepository.saveAndFlush(activeAdmin);

        String accessToken =
                jwtService.generateAccessToken(activeAdmin);

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}",
                                activeUser.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void lockedAdminCannotAccessProtectedEndpoint()
            throws Exception {

        activeAdmin.setStatus(UserStatus.LOCKED);

        activeAdmin =
                userRepository.saveAndFlush(activeAdmin);

        String accessToken =
                jwtService.generateAccessToken(activeAdmin);

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}",
                                activeUser.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isForbidden());
    }
}