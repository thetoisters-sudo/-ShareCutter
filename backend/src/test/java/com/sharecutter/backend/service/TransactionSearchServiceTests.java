package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.exception.InvalidTransactionException;
import com.sharecutter.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionSearchServiceTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private AssetService assetService;

    @Mock
    private PortfolioEntity portfolio;

    @Mock
    private AssetEntity firstAsset;

    @Mock
    private AssetEntity secondAsset;

    @Mock
    private TransactionEntity buyTransaction;

    @Mock
    private TransactionEntity sellTransaction;

    @Mock
    private TransactionEntity depositTransaction;

    private TransactionService transactionService;

    private UUID userId;
    private UUID portfolioId;
    private UUID firstAssetId;
    private UUID secondAssetId;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository,
                portfolioService,
                assetService
        );

        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        firstAssetId = UUID.randomUUID();
        secondAssetId = UUID.randomUUID();
    }

    @Test
    void shouldReturnAllTransactionsWhenNoFiltersProvided() {
        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(
                                buyTransaction,
                                sellTransaction,
                                depositTransaction
                        )
                );

        List<TransactionEntity> result =
                transactionService.searchTransactions(
                        userId,
                        portfolioId,
                        null,
                        null,
                        null,
                        null
                );

        assertThat(result)
                .containsExactly(
                        buyTransaction,
                        sellTransaction,
                        depositTransaction
                );

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(transactionRepository)
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                );

        verify(assetService, never()).getAsset(
                userId,
                portfolioId,
                firstAssetId
        );
    }

    @Test
    void shouldFilterTransactionsByAssetAndTypeAndDateRange() {
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

        OffsetDateTime matchingExecutionTime =
                OffsetDateTime.of(
                        2026,
                        6,
                        15,
                        12,
                        0,
                        0,
                        0,
                        ZoneOffset.UTC
                );

       
        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetService.getAsset(
                userId,
                portfolioId,
                firstAssetId
        )).thenReturn(firstAsset);

        when(firstAsset.getId())
                .thenReturn(firstAssetId);

        

        when(buyTransaction.getAsset())
                .thenReturn(firstAsset);

        when(buyTransaction.getTransactionType())
                .thenReturn(TransactionType.BUY);

        when(buyTransaction.getExecutedAt())
                .thenReturn(matchingExecutionTime);

        when(sellTransaction.getAsset())
                .thenReturn(firstAsset);

        when(sellTransaction.getTransactionType())
                .thenReturn(TransactionType.SELL);

        when(depositTransaction.getAsset())
                .thenReturn(null);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(
                                buyTransaction,
                                sellTransaction,
                                depositTransaction
                        )
                );

        List<TransactionEntity> result =
                transactionService.searchTransactions(
                        userId,
                        portfolioId,
                        firstAssetId,
                        TransactionType.BUY,
                        startDate,
                        endDate
                );

        assertThat(result)
                .containsExactly(
                        buyTransaction
                );

        verify(assetService).getAsset(
                userId,
                portfolioId,
                firstAssetId
        );

        verify(transactionRepository)
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                );
    }

    @Test
    void shouldFilterTransactionsByAssetOnly() {
        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetService.getAsset(
                userId,
                portfolioId,
                firstAssetId
        )).thenReturn(firstAsset);

        when(firstAsset.getId())
                .thenReturn(firstAssetId);

        when(secondAsset.getId())
                .thenReturn(secondAssetId);

        when(buyTransaction.getAsset())
                .thenReturn(firstAsset);

        when(sellTransaction.getAsset())
                .thenReturn(secondAsset);

        when(depositTransaction.getAsset())
                .thenReturn(null);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(
                                buyTransaction,
                                sellTransaction,
                                depositTransaction
                        )
                );

        List<TransactionEntity> result =
                transactionService.searchTransactions(
                        userId,
                        portfolioId,
                        firstAssetId,
                        null,
                        null,
                        null
                );

        assertThat(result)
                .containsExactly(
                        buyTransaction
                );

        verify(assetService).getAsset(
                userId,
                portfolioId,
                firstAssetId
        );
    }

    @Test
    void shouldFilterTransactionsByTypeOnly() {
        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(buyTransaction.getTransactionType())
                .thenReturn(TransactionType.BUY);

        when(sellTransaction.getTransactionType())
                .thenReturn(TransactionType.SELL);

        when(depositTransaction.getTransactionType())
                .thenReturn(TransactionType.DEPOSIT);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(
                                buyTransaction,
                                sellTransaction,
                                depositTransaction
                        )
                );

        List<TransactionEntity> result =
                transactionService.searchTransactions(
                        userId,
                        portfolioId,
                        null,
                        TransactionType.SELL,
                        null,
                        null
                );

        assertThat(result)
                .containsExactly(
                        sellTransaction
                );

        verify(assetService, never()).getAsset(
                userId,
                portfolioId,
                firstAssetId
        );
    }

    @Test
    void shouldIncludeTransactionsOnDateRangeBoundaries() {
        OffsetDateTime startDate =
                OffsetDateTime.of(
                        2026,
                        3,
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
                        3,
                        31,
                        23,
                        59,
                        59,
                        0,
                        ZoneOffset.UTC
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(buyTransaction.getExecutedAt())
                .thenReturn(startDate);

        when(sellTransaction.getExecutedAt())
                .thenReturn(endDate);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(
                                buyTransaction,
                                sellTransaction
                        )
                );

        List<TransactionEntity> result =
                transactionService.searchTransactions(
                        userId,
                        portfolioId,
                        null,
                        null,
                        startDate,
                        endDate
                );

        assertThat(result)
                .containsExactly(
                        buyTransaction,
                        sellTransaction
                );
    }

    @Test
    void shouldRejectSearchWithStartDateOnly() {
        OffsetDateTime startDate =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                ).minusDays(30);

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> transactionService.searchTransactions(
                        userId,
                        portfolioId,
                        null,
                        null,
                        startDate,
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Transaction start date and end date "
                                + "must be provided together"
                );

        verify(transactionRepository, never())
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                );
    }

    @Test
    void shouldRejectSearchWithEndDateOnly() {
        OffsetDateTime endDate =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> transactionService.searchTransactions(
                        userId,
                        portfolioId,
                        null,
                        null,
                        null,
                        endDate
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Transaction start date and end date "
                                + "must be provided together"
                );

        verify(transactionRepository, never())
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                );
    }

    @Test
    void shouldRejectSearchWithReversedDateRange() {
        OffsetDateTime startDate =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        OffsetDateTime endDate =
                startDate.minusDays(1);

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> transactionService.searchTransactions(
                        userId,
                        portfolioId,
                        null,
                        null,
                        startDate,
                        endDate
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Transaction start date must not be after end date"
                );

        verify(transactionRepository, never())
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                );
    }
}