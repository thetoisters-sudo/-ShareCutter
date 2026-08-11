package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchaseExecuteRequest;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchaseExecutionResponse;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchasePreviewRequest;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchasePreviewResponse;
import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.exception.InvalidTransactionException;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.service.marketdata.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(
        strictness = Strictness.LENIENT
)
class PortfolioAllocationPurchaseServiceTests {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private AssetService assetService;

    @Mock
    private PortfolioHoldingRepository
            portfolioHoldingRepository;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TransactionEntity createdTransaction;

    @Mock
    private PortfolioEntity portfolio;

    @Mock
    private AssetEntity selectedAsset;

    @Mock
    private AssetEntity otherAsset;

    @Mock
    private PortfolioHoldingEntity selectedHolding;

    @Mock
    private PortfolioHoldingEntity otherHolding;

    private PortfolioAllocationPurchaseService service;

    private UUID userId;

    private UUID portfolioId;

    private UUID selectedAssetId;

    private UUID otherAssetId;

    @BeforeEach
    void setUp() {
        service =
                new PortfolioAllocationPurchaseService(
                        portfolioService,
                        assetService,
                        portfolioHoldingRepository,
                        marketDataService,
                        transactionService
                );

        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        selectedAssetId = UUID.randomUUID();
        otherAssetId = UUID.randomUUID();

        when(
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                )
        ).thenReturn(
                portfolio
        );

        when(
                portfolio.getId()
        ).thenReturn(
                portfolioId
        );

        when(
                portfolio.getName()
        ).thenReturn(
                "Growth Portfolio"
        );

        when(
                portfolio.getCreationMethod()
        ).thenReturn(
                PortfolioCreationMethod.BY_AMOUNT
        );

        when(
                portfolio.getInitialValue()
        ).thenReturn(
                new BigDecimal(
                        "10000.0000"
                )
        );

        when(
                assetService.getAsset(
                        userId,
                        portfolioId,
                        selectedAssetId
                )
        ).thenReturn(
                selectedAsset
        );

        when(
                selectedAsset.getId()
        ).thenReturn(
                selectedAssetId
        );

        when(
                selectedAsset.getSymbol()
        ).thenReturn(
                "TTWO"
        );

        when(
                selectedAsset.getDisplayName()
        ).thenReturn(
                "Take-Two Interactive Software, Inc."
        );

        when(
                selectedAsset.getExchange()
        ).thenReturn(
                "NASDAQ"
        );

        when(
                selectedAsset.getCurrency()
        ).thenReturn(
                "USD"
        );

        when(
                marketDataService.getLatestPrice(
                        "TTWO",
                        "NASDAQ"
                )
        ).thenReturn(
                new MarketPriceResponse(
                        "TTWO",
                        "NASDAQ",
                        new BigDecimal(
                                "205.55"
                        ),
                        OffsetDateTime.now(
                                ZoneOffset.UTC
                        )
                )
        );
    }

    @Test
    void shouldCalculateFractionalQuantityForNewHolding() {
        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of()
        );

        AllocationPurchasePreviewResponse response =
                service.previewPurchase(
                        userId,
                        portfolioId,
                        new AllocationPurchasePreviewRequest(
                                selectedAssetId,
                                new BigDecimal(
                                        "12.5"
                                )
                        )
                );

        assertThat(
                response.portfolioInitialValue()
        ).isEqualByComparingTo(
                "10000.00000000"
        );

        assertThat(
                response.currentPrice()
        ).isEqualByComparingTo(
                "205.55000000"
        );

        assertThat(
                response.targetWeightPercent()
        ).isEqualByComparingTo(
                "12.500000"
        );

        assertThat(
                response.targetMarketValue()
        ).isEqualByComparingTo(
                "1250.00000000"
        );

        assertThat(
                response.existingQuantity()
        ).isEqualByComparingTo(
                "0.00000000"
        );

        assertThat(
                response.targetQuantity()
        ).isEqualByComparingTo(
                "6.08124543"
        );

        assertThat(
                response.quantityToBuy()
        ).isEqualByComparingTo(
                "6.08124543"
        );

        assertThat(
                response.estimatedPurchaseAmount()
        ).isEqualByComparingTo(
                "1249.99999814"
        );

        assertThat(
                response.remainingAssignableWeightPercent()
        ).isEqualByComparingTo(
                "87.500000"
        );

        assertThat(
                response.suggestedAction()
        ).isEqualTo(
                "BUY"
        );
    }

    @Test
    void shouldSubtractExistingQuantityFromTargetQuantity() {
        when(
                selectedHolding.getAsset()
        ).thenReturn(
                selectedAsset
        );

        when(
                selectedHolding.getQuantity()
        ).thenReturn(
                new BigDecimal(
                        "2.00000000"
                )
        );

        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of(
                        selectedHolding
                )
        );

        AllocationPurchasePreviewResponse response =
                service.previewPurchase(
                        userId,
                        portfolioId,
                        new AllocationPurchasePreviewRequest(
                                selectedAssetId,
                                new BigDecimal(
                                        "12.5"
                                )
                        )
                );

        assertThat(
                response.existingQuantity()
        ).isEqualByComparingTo(
                "2.00000000"
        );

        assertThat(
                response.targetQuantity()
        ).isEqualByComparingTo(
                "6.08124543"
        );

        assertThat(
                response.quantityToBuy()
        ).isEqualByComparingTo(
                "4.08124543"
        );

        assertThat(
                response.estimatedPurchaseAmount()
        ).isEqualByComparingTo(
                "838.89999814"
        );

        assertThat(
                response.suggestedAction()
        ).isEqualTo(
                "BUY"
        );
    }

    @Test
    void shouldIncludeOtherAssetTargetWeights() {
        when(
                otherHolding.getAsset()
        ).thenReturn(
                otherAsset
        );

        when(
                otherAsset.getId()
        ).thenReturn(
                otherAssetId
        );

        when(
                otherHolding.getTargetWeightPercent()
        ).thenReturn(
                new BigDecimal(
                        "35.000000"
                )
        );

        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of(
                        otherHolding
                )
        );

        AllocationPurchasePreviewResponse response =
                service.previewPurchase(
                        userId,
                        portfolioId,
                        new AllocationPurchasePreviewRequest(
                                selectedAssetId,
                                new BigDecimal(
                                        "25"
                                )
                        )
                );

        assertThat(
                response.currentlyAssignedWeightPercent()
        ).isEqualByComparingTo(
                "35.000000"
        );

        assertThat(
                response.remainingAssignableWeightPercent()
        ).isEqualByComparingTo(
                "40.000000"
        );
    }

    @Test
    void shouldRejectTargetWeightThatExceedsRemainingAllocation() {
        when(
                otherHolding.getAsset()
        ).thenReturn(
                otherAsset
        );

        when(
                otherAsset.getId()
        ).thenReturn(
                otherAssetId
        );

        when(
                otherHolding.getTargetWeightPercent()
        ).thenReturn(
                new BigDecimal(
                        "80.000000"
                )
        );

        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of(
                        otherHolding
                )
        );

        assertThatThrownBy(
                () ->
                        service.previewPurchase(
                                userId,
                                portfolioId,
                                new AllocationPurchasePreviewRequest(
                                        selectedAssetId,
                                        new BigDecimal(
                                                "25"
                                        )
                                )
                        )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Remaining weight: 20.000000%"
                );
    }

    @Test
    void shouldRejectPortfolioCreatedByHoldings() {
        when(
                portfolio.getCreationMethod()
        ).thenReturn(
                PortfolioCreationMethod.BY_HOLDINGS
        );

        assertThatThrownBy(
                () ->
                        service.previewPurchase(
                                userId,
                                portfolioId,
                                new AllocationPurchasePreviewRequest(
                                        selectedAssetId,
                                        new BigDecimal(
                                                "10"
                                        )
                                )
                        )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "available only for portfolios created by amount"
                );
    }

    @Test
    void shouldRejectZeroMarketPrice() {
        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                marketDataService.getLatestPrice(
                        "TTWO",
                        "NASDAQ"
                )
        ).thenReturn(
                new MarketPriceResponse(
                        "TTWO",
                        "NASDAQ",
                        BigDecimal.ZERO,
                        OffsetDateTime.now(
                                ZoneOffset.UTC
                        )
                )
        );

        assertThatThrownBy(
                () ->
                        service.previewPurchase(
                                userId,
                                portfolioId,
                                new AllocationPurchasePreviewRequest(
                                        selectedAssetId,
                                        new BigDecimal(
                                                "10"
                                        )
                                )
                        )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "positive current market price"
                );
    }

    @Test
    void shouldSuggestSellWhenExistingQuantityExceedsTarget() {
        when(
                selectedHolding.getAsset()
        ).thenReturn(
                selectedAsset
        );

        when(
                selectedHolding.getQuantity()
        ).thenReturn(
                new BigDecimal(
                        "10.00000000"
                )
        );

        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of(
                        selectedHolding
                )
        );

        AllocationPurchasePreviewResponse response =
                service.previewPurchase(
                        userId,
                        portfolioId,
                        new AllocationPurchasePreviewRequest(
                                selectedAssetId,
                                new BigDecimal(
                                        "10"
                                )
                        )
                );

        assertThat(
                response.targetQuantity()
        ).isEqualByComparingTo(
                "4.86499635"
        );

        assertThat(
                response.quantityToBuy()
        ).isEqualByComparingTo(
                "0.00000000"
        );

        assertThat(
                response.estimatedPurchaseAmount()
        ).isEqualByComparingTo(
                "0.00000000"
        );

        assertThat(
                response.suggestedAction()
        ).isEqualTo(
                "SELL_REQUIRED"
        );
    }

    @Test
    void shouldRequestCurrentPriceForSelectedAsset() {
        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of()
        );

        service.previewPurchase(
                userId,
                portfolioId,
                new AllocationPurchasePreviewRequest(
                        selectedAssetId,
                        new BigDecimal(
                                "10"
                        )
                )
        );

        verify(
                marketDataService
        ).getLatestPrice(
                "TTWO",
                "NASDAQ"
        );
    }

    @Test
    void shouldExecuteFractionalPurchaseAndSaveTargetWeight() {
        UUID transactionId =
                UUID.randomUUID();

        when(
                portfolio.getCurrentValue()
        ).thenReturn(
                new BigDecimal(
                        "10000.0000"
                )
        );

        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                portfolioHoldingRepository
                        .sumMarketValueByPortfolioId(
                                portfolioId
                        )
        ).thenReturn(
                BigDecimal.ZERO
        );

        when(
                transactionService.createTransaction(
                        eq(userId),
                        eq(portfolioId),
                        eq(selectedAssetId),
                        eq(TransactionType.BUY),
                        argThat(
                                value ->
                                        value != null
                                                && value.compareTo(
                                                new BigDecimal(
                                                        "6.08124543"
                                                )
                                        ) == 0
                        ),
                        argThat(
                                value ->
                                        value != null
                                                && value.compareTo(
                                                new BigDecimal(
                                                        "205.55000000"
                                                )
                                        ) == 0
                        ),
                        argThat(
                                value ->
                                        value != null
                                                && value.compareTo(
                                                new BigDecimal(
                                                        "1.25000000"
                                                )
                                        ) == 0
                        ),
                        argThat(
                                value ->
                                        value != null
                                                && value.compareTo(
                                                new BigDecimal(
                                                        "1249.99999814"
                                                )
                                        ) == 0
                        ),
                        eq("USD"),
                        any(OffsetDateTime.class),
                        anyString()
                )
        ).thenReturn(
                createdTransaction
        );

        when(
                createdTransaction.getId()
        ).thenReturn(
                transactionId
        );

        when(
                portfolioHoldingRepository
                        .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                                portfolioId,
                                selectedAssetId
                        )
        ).thenReturn(
                Optional.of(
                        selectedHolding
                )
        );

        AllocationPurchaseExecutionResponse response =
                service.executePurchase(
                        userId,
                        portfolioId,
                        new AllocationPurchaseExecuteRequest(
                                selectedAssetId,
                                new BigDecimal(
                                        "12.5"
                                ),
                                new BigDecimal(
                                        "1.25"
                                )
                        )
                );

        assertThat(
                response.transactionId()
        ).isEqualTo(
                transactionId
        );

        assertThat(
                response.purchasedQuantity()
        ).isEqualByComparingTo(
                "6.08124543"
        );

        assertThat(
                response.unitPrice()
        ).isEqualByComparingTo(
                "205.55000000"
        );

        assertThat(
                response.purchaseAmount()
        ).isEqualByComparingTo(
                "1249.99999814"
        );

        assertThat(
                response.fee()
        ).isEqualByComparingTo(
                "1.25000000"
        );

        assertThat(
                response.totalCashUsed()
        ).isEqualByComparingTo(
                "1251.24999814"
        );

        assertThat(
                response.availableCashBefore()
        ).isEqualByComparingTo(
                "10000.00000000"
        );

        assertThat(
                response.availableCashAfter()
        ).isEqualByComparingTo(
                "8748.75000186"
        );

        assertThat(
                response.targetWeightPercent()
        ).isEqualByComparingTo(
                "12.500000"
        );

        verify(
                selectedHolding
        ).updateTargetWeightPercent(
                argThat(
                        value ->
                                value != null
                                        && value.compareTo(
                                        new BigDecimal(
                                                "12.500000"
                                        )
                                ) == 0
                )
        );

        verify(
                portfolioHoldingRepository
        ).save(
                selectedHolding
        );
    }

    @Test
    void shouldTreatNullFeeAsZeroWhenExecutingPurchase() {
        UUID transactionId =
                UUID.randomUUID();

        when(
                portfolio.getCurrentValue()
        ).thenReturn(
                new BigDecimal(
                        "10000.0000"
                )
        );

        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                portfolioHoldingRepository
                        .sumMarketValueByPortfolioId(
                                portfolioId
                        )
        ).thenReturn(
                BigDecimal.ZERO
        );

        when(
                transactionService.createTransaction(
                        eq(userId),
                        eq(portfolioId),
                        eq(selectedAssetId),
                        eq(TransactionType.BUY),
                        any(BigDecimal.class),
                        any(BigDecimal.class),
                        argThat(
                                value ->
                                        value != null
                                                && value.compareTo(
                                                BigDecimal.ZERO
                                        ) == 0
                        ),
                        any(BigDecimal.class),
                        eq("USD"),
                        any(OffsetDateTime.class),
                        anyString()
                )
        ).thenReturn(
                createdTransaction
        );

        when(
                createdTransaction.getId()
        ).thenReturn(
                transactionId
        );

        when(
                portfolioHoldingRepository
                        .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                                portfolioId,
                                selectedAssetId
                        )
        ).thenReturn(
                Optional.of(
                        selectedHolding
                )
        );

        AllocationPurchaseExecutionResponse response =
                service.executePurchase(
                        userId,
                        portfolioId,
                        new AllocationPurchaseExecuteRequest(
                                selectedAssetId,
                                new BigDecimal(
                                        "10"
                                ),
                                null
                        )
                );

        assertThat(
                response.fee()
        ).isEqualByComparingTo(
                "0.00000000"
        );

        assertThat(
                response.totalCashUsed()
        ).isEqualByComparingTo(
                response.purchaseAmount()
        );
    }

    @Test
    void shouldRejectPurchaseWhenPortfolioCashIsInsufficient() {
        when(
                portfolio.getCurrentValue()
        ).thenReturn(
                new BigDecimal(
                        "1000.0000"
                )
        );

        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                portfolioHoldingRepository
                        .sumMarketValueByPortfolioId(
                                portfolioId
                        )
        ).thenReturn(
                new BigDecimal(
                        "900.00000000"
                )
        );

        assertThatThrownBy(
                () ->
                        service.executePurchase(
                                userId,
                                portfolioId,
                                new AllocationPurchaseExecuteRequest(
                                        selectedAssetId,
                                        new BigDecimal(
                                                "20"
                                        ),
                                        BigDecimal.ZERO
                                )
                        )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Insufficient portfolio cash"
                )
                .hasMessageContaining(
                        "Available cash: 100.00000000"
                );

        verify(
                transactionService,
                never()
        ).createTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );

        verify(
                portfolioHoldingRepository,
                never()
        ).save(
                any(
                        PortfolioHoldingEntity.class
                )
        );
    }

    @Test
    void shouldRejectExecutionWhenTargetRequiresSelling() {
        when(
                selectedHolding.getAsset()
        ).thenReturn(
                selectedAsset
        );

        when(
                selectedHolding.getQuantity()
        ).thenReturn(
                new BigDecimal(
                        "10.00000000"
                )
        );

        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of(
                        selectedHolding
                )
        );

        assertThatThrownBy(
                () ->
                        service.executePurchase(
                                userId,
                                portfolioId,
                                new AllocationPurchaseExecuteRequest(
                                        selectedAssetId,
                                        new BigDecimal(
                                                "10"
                                        ),
                                        BigDecimal.ZERO
                                )
                        )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "does not require an additional purchase"
                )
                .hasMessageContaining(
                        "SELL_REQUIRED"
                );

        verify(
                transactionService,
                never()
        ).createTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldRejectExecutionWhenHoldingWasNotCreated() {
        when(
                portfolio.getCurrentValue()
        ).thenReturn(
                new BigDecimal(
                        "10000.0000"
                )
        );

        when(
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                portfolioHoldingRepository
                        .sumMarketValueByPortfolioId(
                                portfolioId
                        )
        ).thenReturn(
                BigDecimal.ZERO
        );

        when(
                transactionService.createTransaction(
                        eq(userId),
                        eq(portfolioId),
                        eq(selectedAssetId),
                        eq(TransactionType.BUY),
                        any(BigDecimal.class),
                        any(BigDecimal.class),
                        any(BigDecimal.class),
                        any(BigDecimal.class),
                        eq("USD"),
                        any(OffsetDateTime.class),
                        anyString()
                )
        ).thenReturn(
                createdTransaction
        );

        when(
                portfolioHoldingRepository
                        .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                                portfolioId,
                                selectedAssetId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(
                () ->
                        service.executePurchase(
                                userId,
                                portfolioId,
                                new AllocationPurchaseExecuteRequest(
                                        selectedAssetId,
                                        new BigDecimal(
                                                "10"
                                        ),
                                        BigDecimal.ZERO
                                )
                        )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Portfolio holding was not created"
                );

        verify(
                portfolioHoldingRepository,
                never()
        ).save(
                any(
                        PortfolioHoldingEntity.class
                )
        );
    }
}