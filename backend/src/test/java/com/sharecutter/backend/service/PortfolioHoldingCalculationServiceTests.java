package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.exception.InvalidTransactionException;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortfolioHoldingCalculationServiceTests {

    private static final UUID USER_ID =
            UUID.randomUUID();

    private static final UUID PORTFOLIO_ID =
            UUID.randomUUID();

    private static final UUID ASSET_ID =
            UUID.randomUUID();

    @Mock
    private PortfolioHoldingRepository
            portfolioHoldingRepository;

    @Mock
    private TransactionRepository
            transactionRepository;

    @Mock
    private PortfolioService
            portfolioService;

    @Mock
    private PortfolioEntity portfolio;

    @Mock
    private AssetEntity asset;

    private PortfolioHoldingCalculationService
            calculationService;

    @BeforeEach
    void setUp() {
        calculationService =
                new PortfolioHoldingCalculationService(
                        portfolioHoldingRepository,
                        transactionRepository,
                        portfolioService
                );

        when(portfolioService.getPortfolio(
                USER_ID,
                PORTFOLIO_ID
        )).thenReturn(portfolio);

        when(portfolio.getId())
                .thenReturn(PORTFOLIO_ID);

        when(asset.getId())
                .thenReturn(ASSET_ID);

        when(asset.getPortfolio())
                .thenReturn(portfolio);

        when(asset.getSymbol())
                .thenReturn("AAPL");
    }

    @Test
    void shouldCreateHoldingFromFirstBuy() {
        TransactionEntity buy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "10.00000000",
                        "100.00000000",
                        "5.00000000",
                        "1000.00000000",
                        3
                );

        prepareAssetTransactions(
                List.of(buy)
        );

        when(portfolioHoldingRepository
                .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                        PORTFOLIO_ID,
                        ASSET_ID
                ))
                .thenReturn(Optional.empty());

        when(portfolioHoldingRepository.save(
                any(PortfolioHoldingEntity.class)
        ))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        PortfolioHoldingEntity result =
                calculationService
                        .recalculateAssetHolding(
                                USER_ID,
                                PORTFOLIO_ID,
                                asset
                        );

        assertThat(result.getPortfolio())
                .isSameAs(portfolio);

        assertThat(result.getAsset())
                .isSameAs(asset);

        assertThat(result.getQuantity())
                .isEqualByComparingTo(
                        "10.00000000"
                );

        assertThat(result.getAverageCost())
                .isEqualByComparingTo(
                        "100.50000000"
                );

        assertThat(result.getCurrentPrice())
                .isEqualByComparingTo(
                        "100.00000000"
                );

        assertThat(result.getTotalCost())
                .isEqualByComparingTo(
                        "1005.00000000"
                );

        assertThat(result.getMarketValue())
                .isEqualByComparingTo(
                        "1000.00000000"
                );

        assertThat(result.getRealizedProfit())
                .isEqualByComparingTo(
                        "0.00000000"
                );

        assertThat(result.getUnrealizedProfit())
                .isEqualByComparingTo(
                        "-5.00000000"
                );

        assertThat(result.getLastCalculatedAt())
                .isNotNull();

        verify(portfolioHoldingRepository)
                .save(result);
    }

    @Test
    void shouldCalculateWeightedAverageCostForTwoBuys() {
        TransactionEntity firstBuy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "5.00000000",
                        "100.00000000",
                        "0.00000000",
                        "500.00000000",
                        5
                );

        TransactionEntity secondBuy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "3.00000000",
                        "120.00000000",
                        "0.00000000",
                        "360.00000000",
                        2
                );

        prepareAssetTransactions(
                List.of(
                        secondBuy,
                        firstBuy
                )
        );

        PortfolioHoldingEntity existingHolding =
                new PortfolioHoldingEntity(
                        portfolio,
                        asset
                );

        when(portfolioHoldingRepository
                .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                        PORTFOLIO_ID,
                        ASSET_ID
                ))
                .thenReturn(
                        Optional.of(
                                existingHolding
                        )
                );

        when(portfolioHoldingRepository.save(
                existingHolding
        ))
                .thenReturn(existingHolding);

        PortfolioHoldingEntity result =
                calculationService
                        .recalculateAssetHolding(
                                USER_ID,
                                PORTFOLIO_ID,
                                asset
                        );

        assertThat(result.getQuantity())
                .isEqualByComparingTo(
                        "8.00000000"
                );

        assertThat(result.getAverageCost())
                .isEqualByComparingTo(
                        "107.50000000"
                );

        assertThat(result.getCurrentPrice())
                .isEqualByComparingTo(
                        "120.00000000"
                );

        assertThat(result.getTotalCost())
                .isEqualByComparingTo(
                        "860.00000000"
                );

        assertThat(result.getMarketValue())
                .isEqualByComparingTo(
                        "960.00000000"
                );

        assertThat(result.getUnrealizedProfit())
                .isEqualByComparingTo(
                        "100.00000000"
                );
    }

    @Test
    void shouldSupportFractionalShareQuantities() {
        TransactionEntity firstBuy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "0.75000000",
                        "200.00000000",
                        "0.00000000",
                        "150.00000000",
                        4
                );

        TransactionEntity secondBuy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "0.25000000",
                        "240.00000000",
                        "0.00000000",
                        "60.00000000",
                        1
                );

        prepareAssetTransactions(
                List.of(
                        secondBuy,
                        firstBuy
                )
        );

        when(portfolioHoldingRepository
                .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                        PORTFOLIO_ID,
                        ASSET_ID
                ))
                .thenReturn(Optional.empty());

        when(portfolioHoldingRepository.save(
                any(PortfolioHoldingEntity.class)
        ))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        PortfolioHoldingEntity result =
                calculationService
                        .recalculateAssetHolding(
                                USER_ID,
                                PORTFOLIO_ID,
                                asset
                        );

        assertThat(result.getQuantity())
                .isEqualByComparingTo(
                        "1.00000000"
                );

        assertThat(result.getAverageCost())
                .isEqualByComparingTo(
                        "210.00000000"
                );

        assertThat(result.getCurrentPrice())
                .isEqualByComparingTo(
                        "240.00000000"
                );

        assertThat(result.getMarketValue())
                .isEqualByComparingTo(
                        "240.00000000"
                );

        assertThat(result.getUnrealizedProfit())
                .isEqualByComparingTo(
                        "30.00000000"
                );
    }

    @Test
    void shouldCalculatePartialSellAndRealizedProfit() {
        TransactionEntity buy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "10.00000000",
                        "100.00000000",
                        "0.00000000",
                        "1000.00000000",
                        5
                );

        TransactionEntity sell =
                createTradeTransaction(
                        TransactionType.SELL,
                        "4.00000000",
                        "130.00000000",
                        "2.00000000",
                        "520.00000000",
                        1
                );

        prepareAssetTransactions(
                List.of(
                        sell,
                        buy
                )
        );

        when(portfolioHoldingRepository
                .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                        PORTFOLIO_ID,
                        ASSET_ID
                ))
                .thenReturn(Optional.empty());

        when(portfolioHoldingRepository.save(
                any(PortfolioHoldingEntity.class)
        ))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        PortfolioHoldingEntity result =
                calculationService
                        .recalculateAssetHolding(
                                USER_ID,
                                PORTFOLIO_ID,
                                asset
                        );

        assertThat(result.getQuantity())
                .isEqualByComparingTo(
                        "6.00000000"
                );

        assertThat(result.getAverageCost())
                .isEqualByComparingTo(
                        "100.00000000"
                );

        assertThat(result.getCurrentPrice())
                .isEqualByComparingTo(
                        "130.00000000"
                );

        assertThat(result.getTotalCost())
                .isEqualByComparingTo(
                        "600.00000000"
                );

        assertThat(result.getMarketValue())
                .isEqualByComparingTo(
                        "780.00000000"
                );

        assertThat(result.getRealizedProfit())
                .isEqualByComparingTo(
                        "118.00000000"
                );

        assertThat(result.getUnrealizedProfit())
                .isEqualByComparingTo(
                        "180.00000000"
                );
    }

    @Test
    void shouldIncludeDividendInRealizedProfit() {
        TransactionEntity buy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "5.00000000",
                        "100.00000000",
                        "0.00000000",
                        "500.00000000",
                        4
                );

        TransactionEntity dividend =
                createDividendTransaction(
                        "25.00000000",
                        "1.00000000",
                        1
                );

        prepareAssetTransactions(
                List.of(
                        dividend,
                        buy
                )
        );

        when(portfolioHoldingRepository
                .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                        PORTFOLIO_ID,
                        ASSET_ID
                ))
                .thenReturn(Optional.empty());

        when(portfolioHoldingRepository.save(
                any(PortfolioHoldingEntity.class)
        ))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        PortfolioHoldingEntity result =
                calculationService
                        .recalculateAssetHolding(
                                USER_ID,
                                PORTFOLIO_ID,
                                asset
                        );

        assertThat(result.getQuantity())
                .isEqualByComparingTo(
                        "5.00000000"
                );

        assertThat(result.getCurrentPrice())
                .isEqualByComparingTo(
                        "100.00000000"
                );

        assertThat(result.getRealizedProfit())
                .isEqualByComparingTo(
                        "24.00000000"
                );
    }

    @Test
    void shouldRejectSellThatExceedsAvailableQuantity() {
        TransactionEntity buy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "5.00000000",
                        "100.00000000",
                        "0.00000000",
                        "500.00000000",
                        4
                );

        TransactionEntity sell =
                createTradeTransaction(
                        TransactionType.SELL,
                        "6.00000000",
                        "120.00000000",
                        "0.00000000",
                        "720.00000000",
                        1
                );

        prepareAssetTransactions(
                List.of(
                        sell,
                        buy
                )
        );

        assertThatThrownBy(
                () ->
                        calculationService
                                .recalculateAssetHolding(
                                        USER_ID,
                                        PORTFOLIO_ID,
                                        asset
                                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "exceeds the available quantity"
                )
                .hasMessageContaining(
                        "AAPL"
                );

        verify(portfolioHoldingRepository, never())
                .save(
                        any(PortfolioHoldingEntity.class)
                );
    }

    @Test
    void shouldRecalculatePortfolioFromCashAndHoldings() {
        PortfolioHoldingEntity firstHolding =
                holdingWithState(
                        "5.00000000",
                        "100.00000000",
                        "120.00000000",
                        "50.00000000"
                );

        PortfolioHoldingEntity secondHolding =
                holdingWithState(
                        "2.00000000",
                        "200.00000000",
                        "250.00000000",
                        "20.00000000"
                );

        TransactionEntity buy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "10.00000000",
                        "100.00000000",
                        "5.00000000",
                        "1000.00000000",
                        5
                );

        TransactionEntity deposit =
                createCashTransaction(
                        TransactionType.DEPOSIT,
                        "500.00000000",
                        "0.00000000",
                        4
                );

        TransactionEntity withdrawal =
                createCashTransaction(
                        TransactionType.WITHDRAWAL,
                        "200.00000000",
                        "0.00000000",
                        3
                );

        TransactionEntity fee =
                createCashTransaction(
                        TransactionType.FEE,
                        "10.00000000",
                        "0.00000000",
                        2
                );

        when(portfolio.getInitialValue())
                .thenReturn(
                        new BigDecimal(
                                "10000.00000000"
                        )
                );

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(
                                firstHolding,
                                secondHolding
                        )
                );

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(
                                fee,
                                withdrawal,
                                deposit,
                                buy
                        )
                );

        PortfolioEntity result =
                calculationService
                        .recalculatePortfolio(
                                USER_ID,
                                PORTFOLIO_ID
                        );

        assertThat(result)
                .isSameAs(portfolio);

        ArgumentCaptor<BigDecimal>
                currentValueCaptor =
                ArgumentCaptor.forClass(
                        BigDecimal.class
                );

        ArgumentCaptor<BigDecimal>
                realizedProfitCaptor =
                ArgumentCaptor.forClass(
                        BigDecimal.class
                );

        ArgumentCaptor<BigDecimal>
                unrealizedProfitCaptor =
                ArgumentCaptor.forClass(
                        BigDecimal.class
                );

        verify(portfolio)
                .updateCurrentValue(
                        currentValueCaptor.capture()
                );

        verify(portfolio)
                .updateTotalRealizedProfit(
                        realizedProfitCaptor.capture()
                );

        verify(portfolio)
                .updateTotalUnrealizedProfit(
                        unrealizedProfitCaptor.capture()
                );

        /*
         * Cash:
         *
         * 10,000
         * - 1,000 BUY
         * - 5 BUY fee
         * + 500 deposit
         * - 200 withdrawal
         * - 10 fee
         *
         * Cash = 9,285
         *
         * Holdings:
         *
         * 5 ֳ— 120 = 600
         * 2 ֳ— 250 = 500
         *
         * Current value = 9,285 + 1,100 = 10,385
         */

        assertThat(
                currentValueCaptor.getValue()
        )
                .isEqualByComparingTo(
                        "10385.00000000"
                );

        assertThat(
                realizedProfitCaptor.getValue()
        )
                .isEqualByComparingTo(
                        "70.00000000"
                );

        assertThat(
                unrealizedProfitCaptor.getValue()
        )
                .isEqualByComparingTo(
                        "200.00000000"
                );
    }


    @Test
    void shouldRejectBuyThatExceedsAvailableCash() {
        TransactionEntity buy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "10.00000000",
                        "100.00000000",
                        "1.00000000",
                        "1000.00000000",
                        1
                );

        when(portfolio.getInitialValue())
                .thenReturn(
                        new BigDecimal(
                                "1000.00000000"
                        )
                );

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        PORTFOLIO_ID
                ))
                .thenReturn(List.of());

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(buy)
                );

        assertThatThrownBy(
                () ->
                        calculationService
                                .recalculatePortfolio(
                                        USER_ID,
                                        PORTFOLIO_ID
                                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "cash balance must not be negative"
                );

        verify(portfolio, never())
                .updateCurrentValue(
                        any(BigDecimal.class)
                );

        verify(portfolio, never())
                .updateTotalRealizedProfit(
                        any(BigDecimal.class)
                );

        verify(portfolio, never())
                .updateTotalUnrealizedProfit(
                        any(BigDecimal.class)
                );
    }

    @Test
    void shouldRejectWithdrawalThatExceedsAvailableCash() {
        TransactionEntity withdrawal =
                createCashTransaction(
                        TransactionType.WITHDRAWAL,
                        "1000.00000001",
                        "0.00000000",
                        1
                );

        when(portfolio.getInitialValue())
                .thenReturn(
                        new BigDecimal(
                                "1000.00000000"
                        )
                );

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        PORTFOLIO_ID
                ))
                .thenReturn(List.of());

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(withdrawal)
                );

        assertThatThrownBy(
                () ->
                        calculationService
                                .recalculatePortfolio(
                                        USER_ID,
                                        PORTFOLIO_ID
                                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "cash balance must not be negative"
                );

        verify(portfolio, never())
                .updateCurrentValue(
                        any(BigDecimal.class)
                );
    }

    @Test
    void shouldAddNetSellProceedsToAvailableCash() {
        PortfolioHoldingEntity remainingHolding =
                holdingWithState(
                        "6.00000000",
                        "100.00000000",
                        "130.00000000",
                        "118.00000000"
                );

        TransactionEntity buy =
                createTradeTransaction(
                        TransactionType.BUY,
                        "10.00000000",
                        "100.00000000",
                        "0.00000000",
                        "1000.00000000",
                        5
                );

        TransactionEntity sell =
                createTradeTransaction(
                        TransactionType.SELL,
                        "4.00000000",
                        "130.00000000",
                        "2.00000000",
                        "520.00000000",
                        1
                );

        when(portfolio.getInitialValue())
                .thenReturn(
                        new BigDecimal(
                                "1000.00000000"
                        )
                );

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(
                                remainingHolding
                        )
                );

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(
                                sell,
                                buy
                        )
                );

        calculationService
                .recalculatePortfolio(
                        USER_ID,
                        PORTFOLIO_ID
                );

        ArgumentCaptor<BigDecimal>
                currentValueCaptor =
                ArgumentCaptor.forClass(
                        BigDecimal.class
                );

        verify(portfolio)
                .updateCurrentValue(
                        currentValueCaptor.capture()
                );

        /*
         * Cash:
         *
         * 1,000
         * - 1,000 BUY
         * + 520 SELL proceeds
         * - 2 SELL fee
         *
         * Cash = 518
         *
         * Remaining holding:
         *
         * 6 × 130 = 780
         *
         * Current value = 518 + 780 = 1,298
         */

        assertThat(
                currentValueCaptor.getValue()
        )
                .isEqualByComparingTo(
                        "1298.00000000"
                );
    }

    private void prepareAssetTransactions(
            List<TransactionEntity> transactions
    ) {
        when(transactionRepository
                .findAllByAssetIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        ASSET_ID
                ))
                .thenReturn(transactions);

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        PORTFOLIO_ID
                ))
                .thenReturn(List.of());

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        PORTFOLIO_ID
                ))
                .thenReturn(transactions);

        when(portfolio.getInitialValue())
                .thenReturn(
                        new BigDecimal(
                                "10000.00000000"
                        )
                );
    }

    private TransactionEntity createTradeTransaction(
            TransactionType transactionType,
            String quantity,
            String unitPrice,
            String fee,
            String totalAmount,
            int hoursAgo
    ) {
        TransactionEntity transaction =
                org.mockito.Mockito.mock(
                        TransactionEntity.class
                );

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

        when(transaction.getTotalAmount())
                .thenReturn(
                        new BigDecimal(totalAmount)
                );

        when(transaction.getExecutedAt())
                .thenReturn(
                        executionTime(hoursAgo)
                );

        when(transaction.getCreatedAt())
                .thenReturn(
                        executionTime(hoursAgo)
                                .plusMinutes(1)
                );

        return transaction;
    }

    private TransactionEntity createDividendTransaction(
            String totalAmount,
            String fee,
            int hoursAgo
    ) {
        TransactionEntity transaction =
                org.mockito.Mockito.mock(
                        TransactionEntity.class
                );

        when(transaction.getTransactionType())
                .thenReturn(
                        TransactionType.DIVIDEND
                );

        when(transaction.getTotalAmount())
                .thenReturn(
                        new BigDecimal(totalAmount)
                );

        when(transaction.getFee())
                .thenReturn(
                        new BigDecimal(fee)
                );

        when(transaction.getExecutedAt())
                .thenReturn(
                        executionTime(hoursAgo)
                );

        when(transaction.getCreatedAt())
                .thenReturn(
                        executionTime(hoursAgo)
                                .plusMinutes(1)
                );

        return transaction;
    }

    private TransactionEntity createCashTransaction(
            TransactionType transactionType,
            String totalAmount,
            String fee,
            int hoursAgo
    ) {
        TransactionEntity transaction =
                org.mockito.Mockito.mock(
                        TransactionEntity.class
                );

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

        when(transaction.getExecutedAt())
                .thenReturn(
                        executionTime(hoursAgo)
                );

        when(transaction.getCreatedAt())
                .thenReturn(
                        executionTime(hoursAgo)
                                .plusMinutes(1)
                );

        return transaction;
    }

    private PortfolioHoldingEntity holdingWithState(
            String quantity,
            String averageCost,
            String currentPrice,
            String realizedProfit
    ) {
        PortfolioHoldingEntity holding =
                new PortfolioHoldingEntity(
                        portfolio,
                        asset
                );

        holding.replaceCalculatedState(
                new BigDecimal(quantity),
                new BigDecimal(averageCost),
                new BigDecimal(currentPrice),
                new BigDecimal(realizedProfit)
        );

        return holding;
    }

    private OffsetDateTime executionTime(
            int hoursAgo
    ) {
        return OffsetDateTime.now(
                ZoneOffset.UTC
        ).minusHours(hoursAgo);
    }
}