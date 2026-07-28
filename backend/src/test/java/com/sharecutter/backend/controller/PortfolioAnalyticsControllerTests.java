package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.analytics.PortfolioSummaryResponse;
import com.sharecutter.backend.exception.GlobalExceptionHandler;
import com.sharecutter.backend.exception.PortfolioNotFoundException;
import com.sharecutter.backend.security.JwtAuthenticationFilter;
import com.sharecutter.backend.service.PortfolioAnalyticsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioAnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PortfolioAnalyticsControllerTests {

    private static final String PORTFOLIOS_PATH =
            "/api/v1/portfolios";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioAnalyticsService
            portfolioAnalyticsService;

    @MockitoBean
    private JwtAuthenticationFilter
            jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPortfolioSummaryReturnsAnalyticsResponse()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        UserEntity authenticatedUser =
                mockAuthenticatedUser(userId);

        PortfolioSummaryResponse response =
                createSummaryResponse(
                        portfolioId
                );

        when(
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                PORTFOLIOS_PATH
                                        + "/{portfolioId}"
                                        + "/analytics/summary",
                                portfolioId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.portfolioId")
                                .value(
                                        portfolioId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.portfolioName")
                                .value("Main Portfolio")
                )
                .andExpect(
                        jsonPath("$.initialValue")
                                .value(100000.0)
                )
                .andExpect(
                        jsonPath("$.currentValue")
                                .value(112500.0)
                )
                .andExpect(
                        jsonPath("$.totalRealizedProfit")
                                .value(3500.0)
                )
                .andExpect(
                        jsonPath("$.totalUnrealizedProfit")
                                .value(9000.0)
                )
                .andExpect(
                        jsonPath("$.totalProfit")
                                .value(12500.0)
                )
                .andExpect(
                        jsonPath("$.totalReturnPercent")
                                .value(12.5)
                )
                .andExpect(
                        jsonPath("$.activeAssetCount")
                                .value(7)
                )
                .andExpect(
                        jsonPath("$.transactionCount")
                                .value(43)
                )
                .andExpect(
                        jsonPath("$.totalBuyAmount")
                                .value(85000.0)
                )
                .andExpect(
                        jsonPath("$.totalSellAmount")
                                .value(22000.0)
                )
                .andExpect(
                        jsonPath("$.totalDividendAmount")
                                .value(1250.0)
                )
                .andExpect(
                        jsonPath("$.totalFeeAmount")
                                .value(180.0)
                )
                .andExpect(
                        jsonPath("$.totalDepositAmount")
                                .value(100000.0)
                )
                .andExpect(
                        jsonPath("$.totalWithdrawalAmount")
                                .value(5000.0)
                )
                .andExpect(
                        jsonPath("$.netCashFlow")
                                .value(33070.0)
                )
                .andExpect(
                        jsonPath("$.calculatedAt")
                                .value(
                                        "2026-07-28T09:00:00Z"
                                )
                );

        verify(portfolioAnalyticsService)
                .getPortfolioSummary(
                        userId,
                        portfolioId
                );

        verify(authenticatedUser).getId();
    }

    @Test
    void getPortfolioSummaryReturnsNotFoundWhenPortfolioDoesNotExist()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        when(
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        )
        ).thenThrow(
                new PortfolioNotFoundException(
                        portfolioId
                )
        );

        mockMvc.perform(
                        get(
                                PORTFOLIOS_PATH
                                        + "/{portfolioId}"
                                        + "/analytics/summary",
                                portfolioId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                )
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
                                        PORTFOLIOS_PATH
                                                + "/"
                                                + portfolioId
                                                + "/analytics/summary"
                                )
                );

        verify(portfolioAnalyticsService)
                .getPortfolioSummary(
                        userId,
                        portfolioId
                );
    }

    @Test
    void getPortfolioSummaryPassesAuthenticatedUserIdToService()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        PortfolioSummaryResponse response =
                createSummaryResponse(
                        portfolioId
                );

        when(
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                PORTFOLIOS_PATH
                                        + "/{portfolioId}"
                                        + "/analytics/summary",
                                portfolioId
                        )
                )
                .andExpect(status().isOk());

        verify(portfolioAnalyticsService)
                .getPortfolioSummary(
                        userId,
                        portfolioId
                );
    }

    private UserEntity mockAuthenticatedUser(
            UUID userId
    ) {
        UserEntity authenticatedUser =
                mock(UserEntity.class);

        when(authenticatedUser.getId())
                .thenReturn(userId);

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                authenticatedUser,
                                null
                        )
                );

        return authenticatedUser;
    }

    private PortfolioSummaryResponse createSummaryResponse(
            UUID portfolioId
    ) {
        OffsetDateTime calculatedAt =
                OffsetDateTime.of(
                        2026,
                        7,
                        28,
                        9,
                        0,
                        0,
                        0,
                        ZoneOffset.UTC
                );

        return new PortfolioSummaryResponse(
                portfolioId,
                "Main Portfolio",
                new BigDecimal("100000.0000"),
                new BigDecimal("112500.0000"),
                new BigDecimal("3500.0000"),
                new BigDecimal("9000.0000"),
                new BigDecimal("12500.0000"),
                new BigDecimal("12.500000"),
                7L,
                43L,
                new BigDecimal("85000.00000000"),
                new BigDecimal("22000.00000000"),
                new BigDecimal("1250.00000000"),
                new BigDecimal("180.00000000"),
                new BigDecimal("100000.00000000"),
                new BigDecimal("5000.00000000"),
                new BigDecimal("33070.00000000"),
                calculatedAt
        );
    }
}