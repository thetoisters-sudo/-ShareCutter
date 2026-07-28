package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.transaction.TransactionResponse;
import com.sharecutter.backend.dto.transaction.TransactionSearchCriteria;
import com.sharecutter.backend.exception.GlobalExceptionHandler;
import com.sharecutter.backend.mapper.TransactionMapper;
import com.sharecutter.backend.security.JwtAuthenticationFilter;
import com.sharecutter.backend.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTests {

    private static final String PORTFOLIOS_PATH =
            "/api/v1/portfolios";

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private TransactionMapper transactionMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTransactionReturnsCreatedResponse()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        UserEntity authenticatedUser =
                mockAuthenticatedUser(userId);

        TransactionEntity createdTransaction =
                mock(TransactionEntity.class);

        OffsetDateTime executedAt =
                transactionTimestamp();

        TransactionResponse response =
                createResponse(
                        transactionId,
                        portfolioId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("10.00000000"),
                        new BigDecimal("150.25000000"),
                        new BigDecimal("2.50000000"),
                        new BigDecimal("1505.00000000"),
                        "USD",
                        executedAt,
                        "Initial purchase"
                );

        when(
                transactionService.createTransaction(
                        userId,
                        portfolioId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("10.00000000"),
                        new BigDecimal("150.25000000"),
                        new BigDecimal("2.50000000"),
                        new BigDecimal("1505.00000000"),
                        "USD",
                        executedAt,
                        "Initial purchase"
                )
        ).thenReturn(createdTransaction);

        when(
                transactionMapper.toResponse(
                        createdTransaction
                )
        ).thenReturn(response);

        String transactionsPath =
                transactionsPath(portfolioId);

        mockMvc.perform(
                        post(transactionsPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "assetId": "%s",
                                          "transactionType": "BUY",
                                          "quantity": 10.00000000,
                                          "unitPrice": 150.25000000,
                                          "fee": 2.50000000,
                                          "totalAmount": 1505.00000000,
                                          "currency": "USD",
                                          "executedAt": "2026-07-27T18:00:00Z",
                                          "notes": "Initial purchase"
                                        }
                                        """.formatted(assetId)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                transactionsPath
                                        + "/"
                                        + transactionId
                        )
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(transactionId.toString())
                )
                .andExpect(
                        jsonPath("$.portfolioId")
                                .value(portfolioId.toString())
                )
                .andExpect(
                        jsonPath("$.assetId")
                                .value(assetId.toString())
                )
                .andExpect(
                        jsonPath("$.transactionType")
                                .value("BUY")
                )
                .andExpect(
                        jsonPath("$.quantity")
                                .value(10.0)
                )
                .andExpect(
                        jsonPath("$.unitPrice")
                                .value(150.25)
                )
                .andExpect(
                        jsonPath("$.fee")
                                .value(2.5)
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(1505.0)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                )
                .andExpect(
                        jsonPath("$.executedAt")
                                .value(
                                        "2026-07-27T18:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.notes")
                                .value("Initial purchase")
                );

        verify(transactionService)
                .createTransaction(
                        userId,
                        portfolioId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("10.00000000"),
                        new BigDecimal("150.25000000"),
                        new BigDecimal("2.50000000"),
                        new BigDecimal("1505.00000000"),
                        "USD",
                        executedAt,
                        "Initial purchase"
                );

        verify(transactionMapper)
                .toResponse(createdTransaction);

        verify(authenticatedUser).getId();
    }

    @Test
    void createTransactionRejectsMissingTransactionType()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        mockAuthenticatedUser(UUID.randomUUID());

        String transactionsPath =
                transactionsPath(portfolioId);

        mockMvc.perform(
                        post(transactionsPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "assetId": "%s",
                                          "quantity": 10.00000000,
                                          "unitPrice": 150.25000000,
                                          "fee": 2.50000000,
                                          "totalAmount": 1505.00000000,
                                          "currency": "USD",
                                          "executedAt": "2026-07-27T18:00:00Z",
                                          "notes": "Initial purchase"
                                        }
                                        """.formatted(assetId)
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
                                .value(transactionsPath)
                );

        verifyNoInteractions(
                transactionService,
                transactionMapper
        );
    }

    @Test
    void createTransactionRejectsInvalidCurrency()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(UUID.randomUUID());

        String transactionsPath =
                transactionsPath(portfolioId);

        mockMvc.perform(
                        post(transactionsPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "transactionType": "DEPOSIT",
                                          "totalAmount": 1000.00000000,
                                          "currency": "US",
                                          "executedAt": "2026-07-27T18:00:00Z",
                                          "notes": "Cash deposit"
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
                                .value(transactionsPath)
                );

        verifyNoInteractions(
                transactionService,
                transactionMapper
        );
    }

    @Test
    void getTransactionsReturnsPortfolioTransactions()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        TransactionEntity firstTransaction =
                mock(TransactionEntity.class);

        TransactionEntity secondTransaction =
                mock(TransactionEntity.class);

        List<TransactionEntity> transactions =
                List.of(
                        firstTransaction,
                        secondTransaction
                );

        Page<TransactionEntity> transactionPage =
                new PageImpl<>(
                        transactions,
                        PageRequest.of(
                                DEFAULT_PAGE,
                                DEFAULT_PAGE_SIZE
                        ),
                        transactions.size()
                );

        TransactionResponse firstResponse =
                createResponse(
                        UUID.randomUUID(),
                        portfolioId,
                        UUID.randomUUID(),
                        TransactionType.BUY,
                        new BigDecimal("5.00000000"),
                        new BigDecimal("100.00000000"),
                        BigDecimal.ZERO,
                        new BigDecimal("500.00000000"),
                        "USD",
                        transactionTimestamp(),
                        "Buy transaction"
                );

        TransactionResponse secondResponse =
                createResponse(
                        UUID.randomUUID(),
                        portfolioId,
                        null,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("2000.00000000"),
                        "USD",
                        transactionTimestamp(),
                        "Deposit transaction"
                );

        TransactionSearchCriteria expectedCriteria =
                new TransactionSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        DEFAULT_PAGE,
                        DEFAULT_PAGE_SIZE
                );

        when(
                transactionService.searchTransactionsPage(
                        userId,
                        portfolioId,
                        expectedCriteria
                )
        ).thenReturn(transactionPage);

        when(
                transactionMapper.toResponse(
                        firstTransaction
                )
        ).thenReturn(firstResponse);

        when(
                transactionMapper.toResponse(
                        secondTransaction
                )
        ).thenReturn(secondResponse);

        mockMvc.perform(
                        get(
                                transactionsPath(
                                        portfolioId
                                )
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].transactionType"
                        ).value("BUY")
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].totalAmount"
                        ).value(500.0)
                )
                .andExpect(
                        jsonPath(
                                "$.content[1].transactionType"
                        ).value("DEPOSIT")
                )
                .andExpect(
                        jsonPath(
                                "$.content[1].totalAmount"
                        ).value(2000.0)
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(DEFAULT_PAGE)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(DEFAULT_PAGE_SIZE)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.first")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.last")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.hasNext")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.hasPrevious")
                                .value(false)
                );

        verify(transactionService)
                .searchTransactionsPage(
                        userId,
                        portfolioId,
                        expectedCriteria
                );

        verify(transactionMapper)
                .toResponse(firstTransaction);

        verify(transactionMapper)
                .toResponse(secondTransaction);
    }

    @Test
    void getTransactionsPassesCombinedFilters()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        OffsetDateTime startDate =
                OffsetDateTime.of(
                        2026,
                        1,
                        1,
                        0,
                        0,
                        0,
                        0,
                        ZoneOffset.UTC
                );

        OffsetDateTime endDate =
                OffsetDateTime.of(
                        2026,
                        12,
                        31,
                        23,
                        59,
                        59,
                        0,
                        ZoneOffset.UTC
                );

        int page = 2;
        int size = 10;

        mockAuthenticatedUser(userId);

        TransactionEntity transaction =
                mock(TransactionEntity.class);

        List<TransactionEntity> transactions =
                List.of(transaction);

        Page<TransactionEntity> transactionPage =
                new PageImpl<>(
                        transactions,
                        PageRequest.of(
                                page,
                                size
                        ),
                        31
                );

        TransactionResponse response =
                createResponse(
                        UUID.randomUUID(),
                        portfolioId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("3.00000000"),
                        new BigDecimal("120.00000000"),
                        BigDecimal.ZERO,
                        new BigDecimal("360.00000000"),
                        "USD",
                        transactionTimestamp(),
                        "Filtered transaction"
                );

        TransactionSearchCriteria expectedCriteria =
                new TransactionSearchCriteria(
                        assetId,
                        TransactionType.BUY,
                        startDate,
                        endDate,
                        page,
                        size
                );

        when(
                transactionService.searchTransactionsPage(
                        userId,
                        portfolioId,
                        expectedCriteria
                )
        ).thenReturn(transactionPage);

        when(
                transactionMapper.toResponse(
                        transaction
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                transactionsPath(
                                        portfolioId
                                )
                        )
                                .param(
                                        "assetId",
                                        assetId.toString()
                                )
                                .param(
                                        "type",
                                        "BUY"
                                )
                                .param(
                                        "startDate",
                                        "2026-01-01T00:00:00Z"
                                )
                                .param(
                                        "endDate",
                                        "2026-12-31T23:59:59Z"
                                )
                                .param(
                                        "page",
                                        String.valueOf(page)
                                )
                                .param(
                                        "size",
                                        String.valueOf(size)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].assetId")
                                .value(assetId.toString())
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].transactionType"
                        ).value("BUY")
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(page)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(size)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(31)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(4)
                )
                .andExpect(
                        jsonPath("$.first")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.last")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.hasNext")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.hasPrevious")
                                .value(true)
                );

        verify(transactionService)
                .searchTransactionsPage(
                        userId,
                        portfolioId,
                        expectedCriteria
                );

        verify(transactionMapper)
                .toResponse(transaction);
    }

    @Test
    void getTransactionReturnsTransactionResponse()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        TransactionEntity transaction =
                mock(TransactionEntity.class);

        TransactionResponse response =
                createResponse(
                        transactionId,
                        portfolioId,
                        assetId,
                        TransactionType.SELL,
                        new BigDecimal("4.00000000"),
                        new BigDecimal("175.00000000"),
                        new BigDecimal("1.50000000"),
                        new BigDecimal("698.50000000"),
                        "USD",
                        transactionTimestamp(),
                        "Partial sale"
                );

        when(
                transactionService.getTransaction(
                        userId,
                        portfolioId,
                        transactionId
                )
        ).thenReturn(transaction);

        when(
                transactionMapper.toResponse(
                        transaction
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                transactionsPath(portfolioId)
                                        + "/{transactionId}",
                                transactionId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(transactionId.toString())
                )
                .andExpect(
                        jsonPath("$.portfolioId")
                                .value(portfolioId.toString())
                )
                .andExpect(
                        jsonPath("$.assetId")
                                .value(assetId.toString())
                )
                .andExpect(
                        jsonPath("$.transactionType")
                                .value("SELL")
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(698.5)
                )
                .andExpect(
                        jsonPath("$.notes")
                                .value("Partial sale")
                );

        verify(transactionService)
                .getTransaction(
                        userId,
                        portfolioId,
                        transactionId
                );

        verify(transactionMapper)
                .toResponse(transaction);
    }

    @Test
    void updateTransactionReturnsUpdatedTransaction()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        TransactionEntity updatedTransaction =
                mock(TransactionEntity.class);

        OffsetDateTime executedAt =
                transactionTimestamp();

        TransactionResponse response =
                createResponse(
                        transactionId,
                        portfolioId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("12.00000000"),
                        new BigDecimal("155.00000000"),
                        new BigDecimal("3.00000000"),
                        new BigDecimal("1863.00000000"),
                        "USD",
                        executedAt,
                        "Updated purchase"
                );

        when(
                transactionService.updateTransaction(
                        userId,
                        portfolioId,
                        transactionId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("12.00000000"),
                        new BigDecimal("155.00000000"),
                        new BigDecimal("3.00000000"),
                        new BigDecimal("1863.00000000"),
                        "USD",
                        executedAt,
                        "Updated purchase"
                )
        ).thenReturn(updatedTransaction);

        when(
                transactionMapper.toResponse(
                        updatedTransaction
                )
        ).thenReturn(response);

        mockMvc.perform(
                        put(
                                transactionsPath(portfolioId)
                                        + "/{transactionId}",
                                transactionId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "assetId": "%s",
                                          "transactionType": "BUY",
                                          "quantity": 12.00000000,
                                          "unitPrice": 155.00000000,
                                          "fee": 3.00000000,
                                          "totalAmount": 1863.00000000,
                                          "currency": "USD",
                                          "executedAt": "2026-07-27T18:00:00Z",
                                          "notes": "Updated purchase"
                                        }
                                        """.formatted(assetId)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(transactionId.toString())
                )
                .andExpect(
                        jsonPath("$.transactionType")
                                .value("BUY")
                )
                .andExpect(
                        jsonPath("$.quantity")
                                .value(12.0)
                )
                .andExpect(
                        jsonPath("$.unitPrice")
                                .value(155.0)
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(1863.0)
                )
                .andExpect(
                        jsonPath("$.notes")
                                .value("Updated purchase")
                );

        verify(transactionService)
                .updateTransaction(
                        userId,
                        portfolioId,
                        transactionId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("12.00000000"),
                        new BigDecimal("155.00000000"),
                        new BigDecimal("3.00000000"),
                        new BigDecimal("1863.00000000"),
                        "USD",
                        executedAt,
                        "Updated purchase"
                );

        verify(transactionMapper)
                .toResponse(updatedTransaction);
    }

    @Test
    void updateTransactionRejectsMissingExecutedAt()
            throws Exception {

        UUID portfolioId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        mockAuthenticatedUser(UUID.randomUUID());

        String requestPath =
                transactionsPath(portfolioId)
                        + "/"
                        + transactionId;

        mockMvc.perform(
                        put(requestPath)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "transactionType": "DEPOSIT",
                                          "totalAmount": 1000.00000000,
                                          "currency": "USD",
                                          "notes": "Updated deposit"
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
                        jsonPath("$.message").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(requestPath)
                );

        verifyNoInteractions(
                transactionService,
                transactionMapper
        );
    }

    @Test
    void deleteTransactionReturnsNoContent()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        mockMvc.perform(
                        delete(
                                transactionsPath(portfolioId)
                                        + "/{transactionId}",
                                transactionId
                        )
                )
                .andExpect(status().isNoContent());

        verify(transactionService)
                .deleteTransaction(
                        userId,
                        portfolioId,
                        transactionId
                );

        verifyNoInteractions(transactionMapper);
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

    private String transactionsPath(
            UUID portfolioId
    ) {
        return PORTFOLIOS_PATH
                + "/"
                + portfolioId
                + "/transactions";
    }

    private OffsetDateTime transactionTimestamp() {
        return OffsetDateTime.of(
                2026,
                7,
                27,
                18,
                0,
                0,
                0,
                ZoneOffset.UTC
        );
    }

    private TransactionResponse createResponse(
            UUID transactionId,
            UUID portfolioId,
            UUID assetId,
            TransactionType transactionType,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal fee,
            BigDecimal totalAmount,
            String currency,
            OffsetDateTime executedAt,
            String notes
    ) {
        OffsetDateTime timestamp =
                transactionTimestamp();

        return new TransactionResponse(
                transactionId,
                portfolioId,
                assetId,
                transactionType,
                quantity,
                unitPrice,
                fee,
                totalAmount,
                currency,
                executedAt,
                notes,
                timestamp,
                timestamp
        );
    }
}