package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.dto.portfolio.PortfolioResponse;
import com.sharecutter.backend.exception.GlobalExceptionHandler;
import com.sharecutter.backend.mapper.PortfolioMapper;
import com.sharecutter.backend.security.JwtAuthenticationFilter;
import com.sharecutter.backend.service.PortfolioService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PortfolioControllerTests {

    private static final String PORTFOLIOS_PATH =
            "/api/v1/portfolios";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioService portfolioService;

    @MockitoBean
    private PortfolioMapper portfolioMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPortfolioReturnsCreatedResponse()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        UserEntity authenticatedUser =
                mockAuthenticatedUser(userId);

        PortfolioEntity createdPortfolio =
                mock(PortfolioEntity.class);

        PortfolioResponse response =
                createResponse(
                        portfolioId,
                        userId,
                        "Main Portfolio",
                        new BigDecimal("10000.0000"),
                        new BigDecimal("10000.0000")
                );

        when(
                portfolioService.createPortfolio(
                        userId,
                        "Main Portfolio",
                        PortfolioCreationMethod.BY_AMOUNT,
                        new BigDecimal("10000.0000")
                )
        ).thenReturn(createdPortfolio);

        when(
                portfolioMapper.toResponse(createdPortfolio)
        ).thenReturn(response);

        mockMvc.perform(
                        post(PORTFOLIOS_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Main Portfolio",
                                          "creationMethod": "BY_AMOUNT",
                                          "initialValue": 10000.0000
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                PORTFOLIOS_PATH + "/" + portfolioId
                        )
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(portfolioId.toString())
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(userId.toString())
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Main Portfolio")
                )
                .andExpect(
                        jsonPath("$.creationMethod")
                                .value("BY_AMOUNT")
                )
                .andExpect(
                        jsonPath("$.initialValue")
                                .value(10000.0)
                )
                .andExpect(
                        jsonPath("$.currentValue")
                                .value(10000.0)
                );

        verify(portfolioService).createPortfolio(
                userId,
                "Main Portfolio",
                PortfolioCreationMethod.BY_AMOUNT,
                new BigDecimal("10000.0000")
        );

        verify(portfolioMapper)
                .toResponse(createdPortfolio);

        verify(authenticatedUser).getId();
    }

    @Test
    void createPortfolioRejectsBlankName()
            throws Exception {

        mockAuthenticatedUser(UUID.randomUUID());

        mockMvc.perform(
                        post(PORTFOLIOS_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": " ",
                                          "creationMethod": "BY_AMOUNT",
                                          "initialValue": 10000.0000
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
                                .value(PORTFOLIOS_PATH)
                );

        verifyNoInteractions(
                portfolioService,
                portfolioMapper
        );
    }

    @Test
    void createPortfolioRejectsZeroInitialValue()
            throws Exception {

        mockAuthenticatedUser(UUID.randomUUID());

        mockMvc.perform(
                        post(PORTFOLIOS_PATH)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Main Portfolio",
                                          "creationMethod": "BY_AMOUNT",
                                          "initialValue": 0
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
                                .value(PORTFOLIOS_PATH)
                );

        verifyNoInteractions(
                portfolioService,
                portfolioMapper
        );
    }

    @Test
    void getPortfoliosReturnsAuthenticatedUserPortfolios()
            throws Exception {

        UUID userId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        PortfolioEntity firstPortfolio =
                mock(PortfolioEntity.class);

        PortfolioEntity secondPortfolio =
                mock(PortfolioEntity.class);

        List<PortfolioEntity> portfolios =
                List.of(
                        firstPortfolio,
                        secondPortfolio
                );

        List<PortfolioResponse> responses =
                List.of(
                        createResponse(
                                UUID.randomUUID(),
                                userId,
                                "Main Portfolio",
                                new BigDecimal("10000.0000"),
                                new BigDecimal("11000.0000")
                        ),
                        createResponse(
                                UUID.randomUUID(),
                                userId,
                                "Retirement Portfolio",
                                new BigDecimal("20000.0000"),
                                new BigDecimal("22000.0000")
                        )
                );

        when(
                portfolioService.getUserPortfolios(userId)
        ).thenReturn(portfolios);

        when(
                portfolioMapper.toResponseList(portfolios)
        ).thenReturn(responses);

        mockMvc.perform(
                        get(PORTFOLIOS_PATH)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()").value(2)
                )
                .andExpect(
                        jsonPath("$[0].name")
                                .value("Main Portfolio")
                )
                .andExpect(
                        jsonPath("$[1].name")
                                .value("Retirement Portfolio")
                );

        verify(portfolioService)
                .getUserPortfolios(userId);

        verify(portfolioMapper)
                .toResponseList(portfolios);
    }

    @Test
    void getPortfolioReturnsPortfolioResponse()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        PortfolioEntity portfolio =
                mock(PortfolioEntity.class);

        PortfolioResponse response =
                createResponse(
                        portfolioId,
                        userId,
                        "Main Portfolio",
                        new BigDecimal("10000.0000"),
                        new BigDecimal("12000.0000")
                );

        when(
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                )
        ).thenReturn(portfolio);

        when(
                portfolioMapper.toResponse(portfolio)
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                PORTFOLIOS_PATH + "/{portfolioId}",
                                portfolioId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(portfolioId.toString())
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Main Portfolio")
                )
                .andExpect(
                        jsonPath("$.currentValue")
                                .value(12000.0)
                );

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(portfolioMapper)
                .toResponse(portfolio);
    }

    @Test
    void renamePortfolioReturnsUpdatedPortfolio()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        PortfolioEntity updatedPortfolio =
                mock(PortfolioEntity.class);

        PortfolioResponse response =
                createResponse(
                        portfolioId,
                        userId,
                        "Updated Portfolio",
                        new BigDecimal("10000.0000"),
                        new BigDecimal("10000.0000")
                );

        when(
                portfolioService.renamePortfolio(
                        userId,
                        portfolioId,
                        "Updated Portfolio"
                )
        ).thenReturn(updatedPortfolio);

        when(
                portfolioMapper.toResponse(updatedPortfolio)
        ).thenReturn(response);

        mockMvc.perform(
                        patch(
                                PORTFOLIOS_PATH +
                                        "/{portfolioId}/name",
                                portfolioId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Updated Portfolio"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(portfolioId.toString())
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Updated Portfolio")
                );

        verify(portfolioService).renamePortfolio(
                userId,
                portfolioId,
                "Updated Portfolio"
        );

        verify(portfolioMapper)
                .toResponse(updatedPortfolio);
    }

    @Test
    void renamePortfolioRejectsBlankName()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(UUID.randomUUID());

        mockMvc.perform(
                        patch(
                                PORTFOLIOS_PATH +
                                        "/{portfolioId}/name",
                                portfolioId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": " "
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
                                .value(
                                        PORTFOLIOS_PATH +
                                                "/" +
                                                portfolioId +
                                                "/name"
                                )
                );

        verifyNoInteractions(
                portfolioService,
                portfolioMapper
        );
    }

    @Test
    void updatePortfolioValueReturnsUpdatedPortfolio()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        PortfolioEntity updatedPortfolio =
                mock(PortfolioEntity.class);

        PortfolioResponse response =
                createResponse(
                        portfolioId,
                        userId,
                        "Main Portfolio",
                        new BigDecimal("10000.0000"),
                        new BigDecimal("12500.0000")
                );

        when(
                portfolioService.updateCurrentValue(
                        userId,
                        portfolioId,
                        new BigDecimal("12500.0000")
                )
        ).thenReturn(updatedPortfolio);

        when(
                portfolioMapper.toResponse(updatedPortfolio)
        ).thenReturn(response);

        mockMvc.perform(
                        patch(
                                PORTFOLIOS_PATH +
                                        "/{portfolioId}/value",
                                portfolioId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "currentValue": 12500.0000
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(portfolioId.toString())
                )
                .andExpect(
                        jsonPath("$.currentValue")
                                .value(12500.0)
                );

        verify(portfolioService).updateCurrentValue(
                userId,
                portfolioId,
                new BigDecimal("12500.0000")
        );

        verify(portfolioMapper)
                .toResponse(updatedPortfolio);
    }

    @Test
    void updatePortfolioValueRejectsNegativeValue()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(UUID.randomUUID());

        mockMvc.perform(
                        patch(
                                PORTFOLIOS_PATH +
                                        "/{portfolioId}/value",
                                portfolioId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "currentValue": -1
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
                                .value(
                                        PORTFOLIOS_PATH +
                                                "/" +
                                                portfolioId +
                                                "/value"
                                )
                );

        verifyNoInteractions(
                portfolioService,
                portfolioMapper
        );
    }

    @Test
    void deletePortfolioReturnsNoContent()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        mockMvc.perform(
                        delete(
                                PORTFOLIOS_PATH +
                                        "/{portfolioId}",
                                portfolioId
                        )
                )
                .andExpect(status().isNoContent());

        verify(portfolioService).deletePortfolio(
                userId,
                portfolioId
        );

        verifyNoInteractions(portfolioMapper);
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

    private PortfolioResponse createResponse(
            UUID portfolioId,
            UUID userId,
            String name,
            BigDecimal initialValue,
            BigDecimal currentValue
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

        BigDecimal totalReturnPercent =
                currentValue
                        .subtract(initialValue)
                        .divide(
                                initialValue,
                                4,
                                java.math.RoundingMode.HALF_UP
                        )
                        .multiply(
                                new BigDecimal("100")
                        );

        return new PortfolioResponse(
                portfolioId,
                userId,
                name,
                PortfolioCreationMethod.BY_AMOUNT,
                initialValue,
                currentValue,
                BigDecimal.ZERO,
                currentValue.subtract(initialValue),
                totalReturnPercent,
                timestamp,
                timestamp
        );
    }
}