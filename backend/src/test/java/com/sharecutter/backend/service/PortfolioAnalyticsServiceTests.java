package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.analytics.PortfolioSummaryResponse;
import com.sharecutter.backend.exception.PortfolioNotFoundException;
import com.sharecutter.backend.repository.AssetRepository;
import com.sharecutter.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioAnalyticsServiceTests {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private PortfolioAnalyticsService portfolioAnalyticsService;

    @BeforeEach
    void setUp() {
        portfolioAnalyticsService =
                new PortfolioAnalyticsService(
                        portfolioService,
                        assetRepository,
                        transactionRepository
                );
    }

    @Test
    void shouldReturnCompletePortfolioSummary() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
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

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                ))
                .thenReturn(7L);

        when(transactionRepository
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                ))
                .thenReturn(43L);

        mockTransactionTotal(
                portfolioId,
                TransactionType.BUY,
                "85000.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.SELL,
                "22000.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.DIVIDEND,
                "1250.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.FEE,
                "180.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.DEPOSIT,
                "100000.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.WITHDRAWAL,
                "5000.00000000"
        );

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
                .isEqualByComparingTo("100000.0000");

        assertThat(result.currentValue())
                .isEqualByComparingTo("112500.0000");

        assertThat(result.totalRealizedProfit())
                .isEqualByComparingTo("3500.0000");

        assertThat(result.totalUnrealizedProfit())
                .isEqualByComparingTo("9000.0000");

        assertThat(result.totalProfit())
                .isEqualByComparingTo("12500.0000");

        assertThat(result.totalReturnPercent())
                .isEqualByComparingTo("12.500000");

        assertThat(result.activeAssetCount())
                .isEqualTo(7L);

        assertThat(result.transactionCount())
                .isEqualTo(43L);

        assertThat(result.totalBuyAmount())
                .isEqualByComparingTo("85000.00000000");

        assertThat(result.totalSellAmount())
                .isEqualByComparingTo("22000.00000000");

        assertThat(result.totalDividendAmount())
                .isEqualByComparingTo("1250.00000000");

        assertThat(result.totalFeeAmount())
                .isEqualByComparingTo("180.00000000");

        assertThat(result.totalDepositAmount())
                .isEqualByComparingTo("100000.00000000");

        assertThat(result.totalWithdrawalAmount())
                .isEqualByComparingTo("5000.00000000");

        assertThat(result.netCashFlow())
                .isEqualByComparingTo("33070.00000000");

        assertThat(result.calculatedAt())
                .isNotNull();

        verify(portfolioService)
                .getPortfolio(
                        userId,
                        portfolioId
                );

        verify(assetRepository)
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                );

        verify(transactionRepository)
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                );

        verifyAllTransactionTypes(portfolioId);
    }

    @Test
    void shouldReturnZeroTotalsWhenNoTransactionsExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                portfolioId,
                "Empty Portfolio",
                "10000.0000"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                ))
                .thenReturn(0L);

        when(transactionRepository
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                ))
                .thenReturn(0L);

        when(transactionRepository
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.BUY
                ))
                .thenReturn(null);

        when(transactionRepository
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.SELL
                ))
                .thenReturn(null);

        when(transactionRepository
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.DIVIDEND
                ))
                .thenReturn(null);

        when(transactionRepository
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.FEE
                ))
                .thenReturn(null);

        when(transactionRepository
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.DEPOSIT
                ))
                .thenReturn(null);

        when(transactionRepository
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.WITHDRAWAL
                ))
                .thenReturn(null);

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
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(result.totalBuyAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(result.totalSellAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(result.totalDividendAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(result.totalFeeAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(result.totalDepositAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(result.totalWithdrawalAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(result.netCashFlow())
                .isEqualByComparingTo(BigDecimal.ZERO);

        verifyAllTransactionTypes(portfolioId);
    }

    @Test
    void shouldCalculateNegativeNetCashFlow() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                portfolioId,
                "Accumulation Portfolio",
                "50000.0000"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                ))
                .thenReturn(3L);

        when(transactionRepository
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                ))
                .thenReturn(8L);

        mockTransactionTotal(
                portfolioId,
                TransactionType.BUY,
                "40000.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.SELL,
                "5000.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.DIVIDEND,
                "500.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.FEE,
                "100.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.DEPOSIT,
                "20000.00000000"
        );

        mockTransactionTotal(
                portfolioId,
                TransactionType.WITHDRAWAL,
                "2000.00000000"
        );

        PortfolioSummaryResponse result =
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        );

        assertThat(result.netCashFlow())
                .isEqualByComparingTo("-16600.00000000");
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

        verifyNoInteractions(assetRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldUseOnlyOwnedPortfolioReturnedByPortfolioService() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                portfolioId,
                "Owned Portfolio",
                "25000.0000"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                ))
                .thenReturn(1L);

        when(transactionRepository
                .countByPortfolioIdAndDeletedAtIsNull(
                        portfolioId
                ))
                .thenReturn(1L);

        for (TransactionType transactionType
                : TransactionType.values()) {

            when(transactionRepository
                    .sumTotalAmountByPortfolioIdAndTransactionType(
                            portfolioId,
                            transactionType
                    ))
                    .thenReturn(BigDecimal.ZERO);
        }

        PortfolioSummaryResponse result =
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                userId,
                                portfolioId
                        );

        assertThat(result.portfolioId())
                .isEqualTo(portfolioId);

        verify(portfolioService)
                .getPortfolio(
                        userId,
                        portfolioId
                );

        verify(transactionRepository, never())
                .countByPortfolioIdAndDeletedAtIsNull(
                        UUID.randomUUID()
                );
    }

    private void mockTransactionTotal(
            UUID portfolioId,
            TransactionType transactionType,
            String amount
    ) {
        when(transactionRepository
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        transactionType
                ))
                .thenReturn(
                        new BigDecimal(amount)
                );
    }

    private void verifyAllTransactionTypes(
            UUID portfolioId
    ) {
        verify(transactionRepository)
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.BUY
                );

        verify(transactionRepository)
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.SELL
                );

        verify(transactionRepository)
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.DIVIDEND
                );

        verify(transactionRepository)
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.FEE
                );

        verify(transactionRepository)
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.DEPOSIT
                );

        verify(transactionRepository)
                .sumTotalAmountByPortfolioIdAndTransactionType(
                        portfolioId,
                        TransactionType.WITHDRAWAL
                );
    }

    private PortfolioEntity createPortfolio(
            UUID portfolioId,
            String portfolioName,
            String initialValue
    ) {
        UserEntity user = new UserEntity(
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