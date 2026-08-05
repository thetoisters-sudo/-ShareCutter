package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.analytics.PortfolioSummaryResponse;
import com.sharecutter.backend.exception.PortfolioNotFoundException;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioAnalyticsServiceTests {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioHoldingRepository
            portfolioHoldingRepository;

    private PortfolioAnalyticsService
            portfolioAnalyticsService;

    @BeforeEach
    void setUp() {
        portfolioAnalyticsService =
                new PortfolioAnalyticsService(
                        portfolioService,
                        transactionRepository,
                        portfolioHoldingRepository
                );
    }

    @Test
    void shouldReturnCompletePortfolioSummary() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Main Portfolio",
                        "100000.0000"
                );

        portfolio.updateCurrentValue(
                new BigDecimal("112500.0000")
        );

        portfolio.updateTotalRealizedProfit(
                new BigDecimal("3500.0000")
        );

        portfolio.updateTotalUnrealizedProfit(
                new BigDecimal("9000.0000")
        );

        List<TransactionEntity> transactions =
                List.of(
                        createTradeTransaction(
                                TransactionType.BUY,
                                "85.00000000",
                                "1000.00000000",
                                "0.00000000"
                        ),
                        createTradeTransaction(
                                TransactionType.SELL,
                                "22.00000000",
                                "1000.00000000",
                                "0.00000000"
                        ),
                        createCashTransaction(
                                TransactionType.DIVIDEND,
                                "1250.00000000",
                                "0.00000000"
                        ),
                        createCashTransaction(
                                TransactionType.FEE,
                                "180.00000000",
                                "0.00000000"
                        ),
                        createCashTransaction(
                                TransactionType.DEPOSIT,
                                "100000.00000000",
                                "0.00000000"
                        ),
                        createCashTransaction(
                                TransactionType.WITHDRAWAL,
                                "5000.00000000",
                                "0.00000000"
                        )
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .countByPortfolioIdAndQuantityGreaterThanAndDeletedAtIsNull(
                        portfolioId,
                        ZERO
                ))
                .thenReturn(7L);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(transactions);

        PortfolioSummaryResponse result =
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        );

        assertThat(result.portfolioId())
                .isEqualTo(portfolioId);

        assertThat(result.portfolioName())
                .isEqualTo("Main Portfolio");

        assertThat(result.initialValue())
                .isEqualByComparingTo(
                        "100000.0000"
                );

        assertThat(result.currentValue())
                .isEqualByComparingTo(
                        "112500.0000"
                );

        assertThat(result.totalRealizedProfit())
                .isEqualByComparingTo(
                        "3500.0000"
                );

        assertThat(result.totalUnrealizedProfit())
                .isEqualByComparingTo(
                        "9000.0000"
                );

        assertThat(result.totalProfit())
                .isEqualByComparingTo(
                        "12500.0000"
                );

        assertThat(result.totalReturnPercent())
                .isEqualByComparingTo(
                        "12.500000"
                );

        assertThat(result.activeAssetCount())
                .isEqualTo(7L);

        assertThat(result.transactionCount())
                .isEqualTo(6L);

        assertThat(result.totalBuyAmount())
                .isEqualByComparingTo(
                        "85000.00000000"
                );

        assertThat(result.totalSellAmount())
                .isEqualByComparingTo(
                        "22000.00000000"
                );

        assertThat(result.totalDividendAmount())
                .isEqualByComparingTo(
                        "1250.00000000"
                );

        assertThat(result.totalFeeAmount())
                .isEqualByComparingTo(
                        "180.00000000"
                );

        assertThat(result.totalDepositAmount())
                .isEqualByComparingTo(
                        "100000.00000000"
                );

        assertThat(result.totalWithdrawalAmount())
                .isEqualByComparingTo(
                        "5000.00000000"
                );

        assertThat(result.netCashFlow())
                .isEqualByComparingTo(
                        "33070.00000000"
                );

        assertThat(result.calculatedAt())
                .isNotNull();

        verify(portfolioService)
                .getPortfolio(
                        userId,
                        portfolioId
                );

        verify(portfolioHoldingRepository)
                .countByPortfolioIdAndQuantityGreaterThanAndDeletedAtIsNull(
                        portfolioId,
                        ZERO
                );

        verify(transactionRepository)
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                );
    }

    @Test
    void shouldCalculateBuyAndSellAmountsFromQuantityAndUnitPrice() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Calculated Transactions",
                        "10000.0000"
                );

        TransactionEntity buy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "2.50000000",
                        "400.00000000",
                        "5.00000000"
                );

        TransactionEntity sell =
                createTradeTransaction(
                        TransactionType.SELL,
                        "0.50000000",
                        "500.00000000",
                        "2.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .countByPortfolioIdAndQuantityGreaterThanAndDeletedAtIsNull(
                        portfolioId,
                        ZERO
                ))
                .thenReturn(1L);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(
                                buy,
                                sell
                        )
                );

        PortfolioSummaryResponse result =
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        );

        assertThat(result.totalBuyAmount())
                .isEqualByComparingTo(
                        "1000.00000000"
                );

        assertThat(result.totalSellAmount())
                .isEqualByComparingTo(
                        "250.00000000"
                );

        assertThat(result.totalFeeAmount())
                .isEqualByComparingTo(
                        "7.00000000"
                );

        assertThat(result.netCashFlow())
                .isEqualByComparingTo(
                        "-757.00000000"
                );
    }

    @Test
    void shouldReturnZeroTotalsWhenNoTransactionsExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Empty Portfolio",
                        "10000.0000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .countByPortfolioIdAndQuantityGreaterThanAndDeletedAtIsNull(
                        portfolioId,
                        ZERO
                ))
                .thenReturn(0L);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(List.of());

        PortfolioSummaryResponse result =
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        );

        assertThat(result.activeAssetCount())
                .isZero();

        assertThat(result.transactionCount())
                .isZero();

        assertThat(result.totalProfit())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalBuyAmount())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalSellAmount())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalDividendAmount())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalFeeAmount())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalDepositAmount())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalWithdrawalAmount())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.netCashFlow())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );
    }

    @Test
    void shouldCalculateNegativeNetCashFlow() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Accumulation Portfolio",
                        "50000.0000"
                );

        List<TransactionEntity> transactions =
                List.of(
                        createTradeTransaction(
                                TransactionType.BUY,
                                "40.00000000",
                                "1000.00000000",
                                "0.00000000"
                        ),
                        createTradeTransaction(
                                TransactionType.SELL,
                                "5.00000000",
                                "1000.00000000",
                                "0.00000000"
                        ),
                        createCashTransaction(
                                TransactionType.DIVIDEND,
                                "500.00000000",
                                "0.00000000"
                        ),
                        createCashTransaction(
                                TransactionType.FEE,
                                "100.00000000",
                                "0.00000000"
                        ),
                        createCashTransaction(
                                TransactionType.DEPOSIT,
                                "20000.00000000",
                                "0.00000000"
                        ),
                        createCashTransaction(
                                TransactionType.WITHDRAWAL,
                                "2000.00000000",
                                "0.00000000"
                        )
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .countByPortfolioIdAndQuantityGreaterThanAndDeletedAtIsNull(
                        portfolioId,
                        ZERO
                ))
                .thenReturn(3L);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(transactions);

        PortfolioSummaryResponse result =
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        );

        assertThat(result.netCashFlow())
                .isEqualByComparingTo(
                        "-16600.00000000"
                );
    }

    @Test
    void shouldNotQueryAnalyticsRepositoriesWhenPortfolioDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenThrow(
                new PortfolioNotFoundException(
                        portfolioId
                )
        );

        assertThatThrownBy(
                () -> portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        )
        )
                .isInstanceOf(
                        PortfolioNotFoundException.class
                )
                .hasMessageContaining(
                        portfolioId.toString()
                );

        verify(portfolioService)
                .getPortfolio(
                        userId,
                        portfolioId
                );

        verifyNoInteractions(
                transactionRepository
        );

        verifyNoInteractions(
                portfolioHoldingRepository
        );
    }

    private TransactionEntity createTradeTransaction(
            TransactionType transactionType,
            String quantity,
            String unitPrice,
            String fee
    ) {
        TransactionEntity transaction =
                mock(TransactionEntity.class);

        when(transaction.getTransactionType())
                .thenReturn(transactionType);

        when(transaction.getQuantity())
                .thenReturn(
                        new BigDecimal(quantity)
                );

        when(transaction.getUnitPrice())
                .thenReturn(
                        new BigDecimal(unitPrice)
                );

        when(transaction.getFee())
                .thenReturn(
                        new BigDecimal(fee)
                );

        return transaction;
    }

    private TransactionEntity createCashTransaction(
            TransactionType transactionType,
            String totalAmount,
            String fee
    ) {
        TransactionEntity transaction =
                mock(TransactionEntity.class);

        when(transaction.getTransactionType())
                .thenReturn(transactionType);

        when(transaction.getTotalAmount())
                .thenReturn(
                        new BigDecimal(totalAmount)
                );

        when(transaction.getFee())
                .thenReturn(
                        new BigDecimal(fee)
                );

        return transaction;
    }

    private PortfolioEntity createPortfolio(
            UUID portfolioId,
            String portfolioName,
            String initialValue
    ) {
        UserEntity user =
                new UserEntity(
                        "analytics@example.com",
                        "hashed-password",
                        "Analytics",
                        "User"
                );

        PortfolioEntity portfolio =
                new PortfolioEntity(
                        user,
                        portfolioName,
                        PortfolioCreationMethod.BY_AMOUNT,
                        new BigDecimal(initialValue)
                );

        ReflectionTestUtils.setField(
                portfolio,
                "id",
                portfolioId
        );

        return portfolio;
    }
}