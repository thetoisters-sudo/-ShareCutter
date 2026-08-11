package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchaseExecuteRequest;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchaseExecutionResponse;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchasePreviewRequest;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchasePreviewResponse;
import com.sharecutter.backend.exception.GlobalExceptionHandler;
import com.sharecutter.backend.security.JwtAuthenticationFilter;
import com.sharecutter.backend.service.PortfolioAllocationPurchaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        PortfolioAllocationPurchaseController.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PortfolioAllocationPurchaseControllerTests {

    private static final String PORTFOLIOS_PATH =
            "/api/v1/portfolios";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioAllocationPurchaseService
            portfolioAllocationPurchaseService;

    @MockitoBean
    private JwtAuthenticationFilter
            jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void previewPurchaseReturnsCalculatedPreview()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        UserEntity authenticatedUser =
                mockAuthenticatedUser(userId);

        AllocationPurchasePreviewResponse response =
                createPreviewResponse(
                        portfolioId,
                        assetId
                );

        when(
                portfolioAllocationPurchaseService
                        .previewPurchase(
                                eq(userId),
                                eq(portfolioId),
                                any(
                                        AllocationPurchasePreviewRequest.class
                                )
                        )
        ).thenReturn(response);

        String requestPath =
                allocationPath(portfolioId)
                        + "/preview";

        mockMvc.perform(
                        post(requestPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "assetId": "%s",
                                          "targetWeightPercent": 12.5
                                        }
                                        """.formatted(assetId)
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
                        jsonPath("$.assetId")
                                .value(assetId.toString())
                )
                .andExpect(
                        jsonPath("$.symbol")
                                .value("TTWO")
                )
                .andExpect(
                        jsonPath("$.displayName")
                                .value(
                                        "Take-Two Interactive Software, Inc."
                                )
                )
                .andExpect(
                        jsonPath("$.exchange")
                                .value("NASDAQ")
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                )
                .andExpect(
                        jsonPath("$.portfolioCurrentValue")
                                .value(10000.0)
                )
                .andExpect(
                        jsonPath("$.currentPrice")
                                .value(205.55)
                )
                .andExpect(
                        jsonPath("$.targetWeightPercent")
                                .value(12.5)
                )
                .andExpect(
                        jsonPath(
                                "$.weightAssignedToOtherAssetsPercent"
                        ).value(25.0)
                )
                .andExpect(
                        jsonPath(
                                "$.allocationDifferencePercent"
                        ).value(62.5)
                )
                .andExpect(
                        jsonPath("$.targetMarketValue")
                                .value(1250.0)
                )
                .andExpect(
                        jsonPath("$.existingQuantity")
                                .value(2.0)
                )
                .andExpect(
                        jsonPath("$.targetQuantity")
                                .value(6.08124543)
                )
                .andExpect(
                        jsonPath("$.quantityToBuy")
                                .value(4.08124543)
                )
                .andExpect(
                        jsonPath("$.estimatedTradeAmount")
                                .value(838.89999814)
                )
                .andExpect(
                        jsonPath("$.suggestedAction")
                                .value("BUY")
                )
                .andExpect(
                        jsonPath("$.calculatedAt")
                                .value(
                                        "2026-08-06T06:00:00Z"
                                )
                );

        verify(
                portfolioAllocationPurchaseService
        ).previewPurchase(
                eq(userId),
                eq(portfolioId),
                any(
                        AllocationPurchasePreviewRequest.class
                )
        );

        verify(authenticatedUser).getId();
    }

    @Test
    void executePurchaseReturnsExecutionResponse()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        UserEntity authenticatedUser =
                mockAuthenticatedUser(userId);

        AllocationPurchaseExecutionResponse response =
                createExecutionResponse(
                        portfolioId,
                        assetId,
                        transactionId
                );

        when(
                portfolioAllocationPurchaseService
                        .executePurchase(
                                eq(userId),
                                eq(portfolioId),
                                any(
                                        AllocationPurchaseExecuteRequest.class
                                )
                        )
        ).thenReturn(response);

        String requestPath =
                allocationPath(portfolioId)
                        + "/execute";

        mockMvc.perform(
                        post(requestPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "assetId": "%s",
                                          "targetWeightPercent": 12.5,
                                          "fee": 1.25
                                        }
                                        """.formatted(assetId)
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
                        jsonPath("$.assetId")
                                .value(assetId.toString())
                )
                .andExpect(
                        jsonPath("$.symbol")
                                .value("TTWO")
                )
                .andExpect(
                        jsonPath("$.transactionId")
                                .value(
                                        transactionId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.targetWeightPercent")
                                .value(12.5)
                )
                .andExpect(
                        jsonPath("$.unitPrice")
                                .value(205.55)
                )
                .andExpect(
                        jsonPath("$.tradedQuantity")
                                .value(6.08124543)
                )
                .andExpect(
                        jsonPath("$.tradeAmount")
                                .value(1249.99999814)
                )
                .andExpect(
                        jsonPath("$.fee")
                                .value(1.25)
                )
                .andExpect(
                        jsonPath("$.cashImpact")
                                .value(-1251.24999814)
                )
                .andExpect(
                        jsonPath("$.availableCashBefore")
                                .value(10000.0)
                )
                .andExpect(
                        jsonPath("$.availableCashAfter")
                                .value(8748.75000186)
                )
                .andExpect(
                        jsonPath(
                                "$.allocationDifferencePercent"
                        ).value(87.5)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                )
                .andExpect(
                        jsonPath("$.executedAt")
                                .value(
                                        "2026-08-06T06:05:00Z"
                                )
                );

        verify(
                portfolioAllocationPurchaseService
        ).executePurchase(
                eq(userId),
                eq(portfolioId),
                any(
                        AllocationPurchaseExecuteRequest.class
                )
        );

        verify(authenticatedUser).getId();
    }

    @Test
    void previewPurchaseRejectsMissingAssetId()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(
                UUID.randomUUID()
        );

        String requestPath =
                allocationPath(portfolioId)
                        + "/preview";

        mockMvc.perform(
                        post(requestPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "targetWeightPercent": 12.5
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(requestPath)
                );

        verifyNoInteractions(
                portfolioAllocationPurchaseService
        );
    }

    @Test
    void previewPurchaseAllowsZeroTargetWeight()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        AllocationPurchasePreviewResponse response =
                createPreviewResponse(
                        portfolioId,
                        assetId
                );

        when(
                portfolioAllocationPurchaseService
                        .previewPurchase(
                                eq(userId),
                                eq(portfolioId),
                                any(
                                        AllocationPurchasePreviewRequest.class
                                )
                        )
        ).thenReturn(response);

        String requestPath =
                allocationPath(portfolioId)
                        + "/preview";

        mockMvc.perform(
                        post(requestPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "assetId": "%s",
                                          "targetWeightPercent": 0
                                        }
                                        """.formatted(assetId)
                                )
                )
                .andExpect(status().isOk());

        verify(
                portfolioAllocationPurchaseService
        ).previewPurchase(
                eq(userId),
                eq(portfolioId),
                any(
                        AllocationPurchasePreviewRequest.class
                )
        );
    }

    @Test
    void previewPurchaseRejectsTargetWeightAboveOneHundred()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser(
                UUID.randomUUID()
        );

        String requestPath =
                allocationPath(portfolioId)
                        + "/preview";

        mockMvc.perform(
                        post(requestPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "assetId": "%s",
                                          "targetWeightPercent": 100.000001
                                        }
                                        """.formatted(assetId)
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(requestPath)
                );

        verifyNoInteractions(
                portfolioAllocationPurchaseService
        );
    }

    @Test
    void executePurchaseRejectsNegativeFee()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser(
                UUID.randomUUID()
        );

        String requestPath =
                allocationPath(portfolioId)
                        + "/execute";

        mockMvc.perform(
                        post(requestPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "assetId": "%s",
                                          "targetWeightPercent": 12.5,
                                          "fee": -1
                                        }
                                        """.formatted(assetId)
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(requestPath)
                );

        verifyNoInteractions(
                portfolioAllocationPurchaseService
        );
    }

    @Test
    void executePurchaseAllowsNullFee()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        AllocationPurchaseExecutionResponse response =
                createExecutionResponse(
                        portfolioId,
                        assetId,
                        transactionId
                );

        when(
                portfolioAllocationPurchaseService
                        .executePurchase(
                                eq(userId),
                                eq(portfolioId),
                                any(
                                        AllocationPurchaseExecuteRequest.class
                                )
                        )
        ).thenReturn(response);

        String requestPath =
                allocationPath(portfolioId)
                        + "/execute";

        mockMvc.perform(
                        post(requestPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "assetId": "%s",
                                          "targetWeightPercent": 12.5
                                        }
                                        """.formatted(assetId)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.transactionId")
                                .value(
                                        transactionId.toString()
                                )
                );

        verify(
                portfolioAllocationPurchaseService
        ).executePurchase(
                eq(userId),
                eq(portfolioId),
                any(
                        AllocationPurchaseExecuteRequest.class
                )
        );
    }

    private UserEntity mockAuthenticatedUser(
            UUID userId
    ) {
        UserEntity authenticatedUser =
                mock(UserEntity.class);

        when(
                authenticatedUser.getId()
        ).thenReturn(userId);

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

    private String allocationPath(
            UUID portfolioId
    ) {
        return PORTFOLIOS_PATH
                + "/"
                + portfolioId
                + "/allocation-purchases";
    }

    private AllocationPurchasePreviewResponse
    createPreviewResponse(
            UUID portfolioId,
            UUID assetId
    ) {
        return new AllocationPurchasePreviewResponse(
                portfolioId,
                "Main Portfolio",
                assetId,
                "TTWO",
                "Take-Two Interactive Software, Inc.",
                "NASDAQ",
                "USD",
                new BigDecimal(
                        "10000.00000000"
                ),
                new BigDecimal(
                        "205.55000000"
                ),
                new BigDecimal(
                        "12.500000"
                ),
                new BigDecimal(
                        "25.000000"
                ),
                new BigDecimal(
                        "37.500000"
                ),
                new BigDecimal(
                        "62.500000"
                ),
                new BigDecimal(
                        "411.10000000"
                ),
                new BigDecimal(
                        "1250.00000000"
                ),
                new BigDecimal(
                        "2.00000000"
                ),
                new BigDecimal(
                        "6.08124543"
                ),
                new BigDecimal(
                        "4.08124543"
                ),
                new BigDecimal(
                        "4.08124543"
                ),
                new BigDecimal(
                        "0.00000000"
                ),
                new BigDecimal(
                        "838.89999814"
                ),
                "BUY",
                OffsetDateTime.of(
                        2026,
                        8,
                        6,
                        6,
                        0,
                        0,
                        0,
                        ZoneOffset.UTC
                )
        );
    }

    private AllocationPurchaseExecutionResponse
    createExecutionResponse(
            UUID portfolioId,
            UUID assetId,
            UUID transactionId
    ) {
        return new AllocationPurchaseExecutionResponse(
                portfolioId,
                "Main Portfolio",
                assetId,
                "TTWO",
                "Take-Two Interactive Software, Inc.",
                transactionId,
                "BUY",
                new BigDecimal(
                        "12.500000"
                ),
                new BigDecimal(
                        "205.55000000"
                ),
                new BigDecimal(
                        "6.08124543"
                ),
                new BigDecimal(
                        "1249.99999814"
                ),
                new BigDecimal(
                        "1.25000000"
                ),
                new BigDecimal(
                        "-1251.24999814"
                ),
                new BigDecimal(
                        "10000.00000000"
                ),
                new BigDecimal(
                        "8748.75000186"
                ),
                new BigDecimal(
                        "12.500000"
                ),
                new BigDecimal(
                        "87.500000"
                ),
                "USD",
                OffsetDateTime.of(
                        2026,
                        8,
                        6,
                        6,
                        5,
                        0,
                        0,
                        ZoneOffset.UTC
                )
        );
    }
}