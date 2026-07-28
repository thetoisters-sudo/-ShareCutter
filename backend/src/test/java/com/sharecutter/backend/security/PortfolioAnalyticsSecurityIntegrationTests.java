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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioAnalyticsSecurityIntegrationTests {

    private static final String USER_EMAIL =
            "analytics.security@example.com";

    private static final String PASSWORD =
            "StrongPassword123";

    private static final String ANALYTICS_PATH =
            "/api/v1/portfolios/{portfolioId}"
                    + "/analytics/summary";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private UserEntity activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new UserEntity(
                USER_EMAIL,
                passwordEncoder.encode(PASSWORD),
                "Analytics",
                "User"
        );

        activeUser.setRole(UserRole.USER);
        activeUser.setStatus(UserStatus.ACTIVE);

        activeUser =
                userRepository.saveAndFlush(activeUser);
    }

    @Test
    void portfolioAnalyticsEndpointRejectsMissingToken()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                ANALYTICS_PATH,
                                portfolioId
                        )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void portfolioAnalyticsEndpointRejectsInvalidToken()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                ANALYTICS_PATH,
                                portfolioId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer invalid-token"
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanReachPortfolioAnalyticsEndpoint()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        String accessToken =
                jwtService.generateAccessToken(
                        activeUser
                );

        mockMvc.perform(
                        get(
                                ANALYTICS_PATH,
                                portfolioId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        org.hamcrest.Matchers
                                                .containsString(
                                                        portfolioId
                                                                .toString()
                                                )
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/v1/portfolios/"
                                                + portfolioId
                                                + "/analytics/summary"
                                )
                );
    }

    @Test
    void portfolioAnalyticsEndpointRejectsRefreshToken()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        String refreshToken =
                jwtService.generateRefreshToken(
                        activeUser
                );

        mockMvc.perform(
                        get(
                                ANALYTICS_PATH,
                                portfolioId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + refreshToken
                                )
                )
                .andExpect(status().isForbidden());
    }
}