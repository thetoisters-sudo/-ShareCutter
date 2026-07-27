package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.dto.asset.AssetResponse;
import com.sharecutter.backend.exception.GlobalExceptionHandler;
import com.sharecutter.backend.mapper.AssetMapper;
import com.sharecutter.backend.security.JwtAuthenticationFilter;
import com.sharecutter.backend.service.AssetService;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AssetControllerTests {

    private static final String PORTFOLIOS_PATH =
            "/api/v1/portfolios";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetService assetService;

    @MockitoBean
    private AssetMapper assetMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAssetReturnsCreatedResponse()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        UserEntity authenticatedUser =
                mockAuthenticatedUser(userId);

        AssetEntity createdAsset =
                mock(AssetEntity.class);

        AssetResponse response =
                createResponse(
                        assetId,
                        portfolioId,
                        "AAPL",
                        "Apple Inc.",
                        AssetType.STOCK,
                        "USD",
                        "US0378331005",
                        "NASDAQ",
                        "Long-term holding"
                );

        when(
                assetService.createAsset(
                        userId,
                        portfolioId,
                        "AAPL",
                        "Apple Inc.",
                        AssetType.STOCK,
                        "USD",
                        "US0378331005",
                        "NASDAQ",
                        "Long-term holding"
                )
        ).thenReturn(createdAsset);

        when(
                assetMapper.toResponse(createdAsset)
        ).thenReturn(response);

        String assetsPath =
                assetsPath(portfolioId);

        mockMvc.perform(
                        post(assetsPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "symbol": "AAPL",
                                          "displayName": "Apple Inc.",
                                          "assetType": "STOCK",
                                          "currency": "USD",
                                          "isin": "US0378331005",
                                          "exchange": "NASDAQ",
                                          "notes": "Long-term holding"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                assetsPath + "/" + assetId
                        )
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(assetId.toString())
                )
                .andExpect(
                        jsonPath("$.portfolioId")
                                .value(portfolioId.toString())
                )
                .andExpect(
                        jsonPath("$.symbol")
                                .value("AAPL")
                )
                .andExpect(
                        jsonPath("$.displayName")
                                .value("Apple Inc.")
                )
                .andExpect(
                        jsonPath("$.assetType")
                                .value("STOCK")
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                )
                .andExpect(
                        jsonPath("$.isin")
                                .value("US0378331005")
                )
                .andExpect(
                        jsonPath("$.exchange")
                                .value("NASDAQ")
                )
                .andExpect(
                        jsonPath("$.notes")
                                .value("Long-term holding")
                );

        verify(assetService).createAsset(
                userId,
                portfolioId,
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "USD",
                "US0378331005",
                "NASDAQ",
                "Long-term holding"
        );

        verify(assetMapper)
                .toResponse(createdAsset);

        verify(authenticatedUser).getId();
    }

    @Test
    void createAssetRejectsBlankSymbol()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(UUID.randomUUID());

        String assetsPath =
                assetsPath(portfolioId);

        mockMvc.perform(
                        post(assetsPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "symbol": " ",
                                          "displayName": "Apple Inc.",
                                          "assetType": "STOCK",
                                          "currency": "USD",
                                          "isin": "US0378331005",
                                          "exchange": "NASDAQ",
                                          "notes": "Long-term holding"
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
                                .value(assetsPath)
                );

        verifyNoInteractions(
                assetService,
                assetMapper
        );
    }

    @Test
    void createAssetRejectsInvalidCurrency()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(UUID.randomUUID());

        String assetsPath =
                assetsPath(portfolioId);

        mockMvc.perform(
                        post(assetsPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "symbol": "AAPL",
                                          "displayName": "Apple Inc.",
                                          "assetType": "STOCK",
                                          "currency": "US",
                                          "isin": "US0378331005",
                                          "exchange": "NASDAQ",
                                          "notes": "Long-term holding"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(assetsPath)
                );

        verifyNoInteractions(
                assetService,
                assetMapper
        );
    }

    @Test
    void getAssetsReturnsPortfolioAssets()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        AssetEntity firstAsset =
                mock(AssetEntity.class);

        AssetEntity secondAsset =
                mock(AssetEntity.class);

        List<AssetEntity> assets =
                List.of(
                        firstAsset,
                        secondAsset
                );

        List<AssetResponse> responses =
                List.of(
                        createResponse(
                                UUID.randomUUID(),
                                portfolioId,
                                "AAPL",
                                "Apple Inc.",
                                AssetType.STOCK,
                                "USD",
                                "US0378331005",
                                "NASDAQ",
                                null
                        ),
                        createResponse(
                                UUID.randomUUID(),
                                portfolioId,
                                "BTC",
                                "Bitcoin",
                                AssetType.CRYPTO,
                                "USD",
                                null,
                                "CRYPTO",
                                null
                        )
                );

        when(
                assetService.getPortfolioAssets(
                        userId,
                        portfolioId
                )
        ).thenReturn(assets);

        when(
                assetMapper.toResponseList(assets)
        ).thenReturn(responses);

        mockMvc.perform(
                        get(assetsPath(portfolioId))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()").value(2)
                )
                .andExpect(
                        jsonPath("$[0].symbol")
                                .value("AAPL")
                )
                .andExpect(
                        jsonPath("$[0].assetType")
                                .value("STOCK")
                )
                .andExpect(
                        jsonPath("$[1].symbol")
                                .value("BTC")
                )
                .andExpect(
                        jsonPath("$[1].assetType")
                                .value("CRYPTO")
                );

        verify(assetService)
                .getPortfolioAssets(
                        userId,
                        portfolioId
                );

        verify(assetMapper)
                .toResponseList(assets);
    }

    @Test
    void getAssetReturnsAssetResponse()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        AssetEntity asset =
                mock(AssetEntity.class);

        AssetResponse response =
                createResponse(
                        assetId,
                        portfolioId,
                        "MSFT",
                        "Microsoft Corporation",
                        AssetType.STOCK,
                        "USD",
                        "US5949181045",
                        "NASDAQ",
                        "Technology holding"
                );

        when(
                assetService.getAsset(
                        userId,
                        portfolioId,
                        assetId
                )
        ).thenReturn(asset);

        when(
                assetMapper.toResponse(asset)
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                assetsPath(portfolioId)
                                        + "/{assetId}",
                                assetId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(assetId.toString())
                )
                .andExpect(
                        jsonPath("$.portfolioId")
                                .value(portfolioId.toString())
                )
                .andExpect(
                        jsonPath("$.symbol")
                                .value("MSFT")
                )
                .andExpect(
                        jsonPath("$.displayName")
                                .value("Microsoft Corporation")
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                );

        verify(assetService).getAsset(
                userId,
                portfolioId,
                assetId
        );

        verify(assetMapper)
                .toResponse(asset);
    }

    @Test
    void updateAssetReturnsUpdatedAsset()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        AssetEntity updatedAsset =
                mock(AssetEntity.class);

        AssetResponse response =
                createResponse(
                        assetId,
                        portfolioId,
                        "MSFT",
                        "Microsoft Corporation",
                        AssetType.STOCK,
                        "USD",
                        "US5949181045",
                        "NASDAQ",
                        "Updated asset"
                );

        when(
                assetService.updateAsset(
                        userId,
                        portfolioId,
                        assetId,
                        "MSFT",
                        "Microsoft Corporation",
                        AssetType.STOCK,
                        "USD",
                        "US5949181045",
                        "NASDAQ",
                        "Updated asset"
                )
        ).thenReturn(updatedAsset);

        when(
                assetMapper.toResponse(updatedAsset)
        ).thenReturn(response);

        mockMvc.perform(
                        put(
                                assetsPath(portfolioId)
                                        + "/{assetId}",
                                assetId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "symbol": "MSFT",
                                          "displayName": "Microsoft Corporation",
                                          "assetType": "STOCK",
                                          "currency": "USD",
                                          "isin": "US5949181045",
                                          "exchange": "NASDAQ",
                                          "notes": "Updated asset"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(assetId.toString())
                )
                .andExpect(
                        jsonPath("$.symbol")
                                .value("MSFT")
                )
                .andExpect(
                        jsonPath("$.displayName")
                                .value("Microsoft Corporation")
                )
                .andExpect(
                        jsonPath("$.notes")
                                .value("Updated asset")
                );

        verify(assetService).updateAsset(
                userId,
                portfolioId,
                assetId,
                "MSFT",
                "Microsoft Corporation",
                AssetType.STOCK,
                "USD",
                "US5949181045",
                "NASDAQ",
                "Updated asset"
        );

        verify(assetMapper)
                .toResponse(updatedAsset);
    }

    @Test
    void updateAssetRejectsMissingAssetType()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser(UUID.randomUUID());

        String requestPath =
                assetsPath(portfolioId)
                        + "/"
                        + assetId;

        mockMvc.perform(
                        put(requestPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "symbol": "MSFT",
                                          "displayName": "Microsoft Corporation",
                                          "currency": "USD",
                                          "isin": "US5949181045",
                                          "exchange": "NASDAQ",
                                          "notes": "Updated asset"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status").value(400)
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
                assetService,
                assetMapper
        );
    }

    @Test
    void deleteAssetReturnsNoContent()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        mockMvc.perform(
                        delete(
                                assetsPath(portfolioId)
                                        + "/{assetId}",
                                assetId
                        )
                )
                .andExpect(status().isNoContent());

        verify(assetService).deleteAsset(
                userId,
                portfolioId,
                assetId
        );

        verifyNoInteractions(assetMapper);
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

    private String assetsPath(
            UUID portfolioId
    ) {
        return PORTFOLIOS_PATH
                + "/"
                + portfolioId
                + "/assets";
    }

    private AssetResponse createResponse(
            UUID assetId,
            UUID portfolioId,
            String symbol,
            String displayName,
            AssetType assetType,
            String currency,
            String isin,
            String exchange,
            String notes
    ) {
        OffsetDateTime timestamp =
                OffsetDateTime.of(
                        2026,
                        7,
                        27,
                        18,
                        0,
                        0,
                        0,
                        ZoneOffset.UTC
                );

        return new AssetResponse(
                assetId,
                portfolioId,
                symbol,
                displayName,
                assetType,
                currency,
                isin,
                exchange,
                notes,
                timestamp,
                timestamp
        );
    }
}