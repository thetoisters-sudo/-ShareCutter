package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.dto.analytics.AssetAllocationItemResponse;
import com.sharecutter.backend.dto.analytics.PortfolioAllocationResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioAllocationServiceTests {

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
    void shouldCalculateAllocationUsingPortfolioValue() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Market Value Portfolio"
                );

        PortfolioHoldingEntity appleHolding =
                createHolding(
                        portfolio,
                        "AAPL",
                        "Apple Inc.",
                        "10.00000000",
                        "100.00000000",
                        "300.00000000",
                        "200.00000000"
                );

        PortfolioHoldingEntity microsoftHolding =
                createHolding(
                        portfolio,
                        "MSFT",
                        "Microsoft Corporation",
                        "10.00000000",
                        "150.00000000",
                        "200.00000000",
                        "100.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(
                                appleHolding,
                                microsoftHolding
                        )
                );

        PortfolioAllocationResponse result =
                portfolioAnalyticsService
                        .getPortfolioAllocation(
                                userId,
                                portfolioId
                        );

        assertThat(result.portfolioValue())
                .isEqualByComparingTo(
                        "10000.0000"
                );

        assertThat(result.totalMarketValue())
                .isEqualByComparingTo(
                        "5000.0000000000000000"
                );

        assertThat(result.cashBalance())
                .isEqualByComparingTo(
                        "5000.0000000000000000"
                );

        assertThat(result.totalCost())
                .isEqualByComparingTo(
                        "2500.0000000000000000"
                );

        assertThat(result.totalRealizedProfit())
                .isEqualByComparingTo(
                        "300.00000000"
                );

        assertThat(result.totalUnrealizedProfit())
                .isEqualByComparingTo(
                        "2500.0000000000000000"
                );

        assertThat(result.allocatedAssetCount())
                .isEqualTo(2L);

        assertThat(result.assets())
                .hasSize(2);

        AssetAllocationItemResponse apple =
                result.assets().get(0);

        AssetAllocationItemResponse microsoft =
                result.assets().get(1);

        assertThat(apple.symbol())
                .isEqualTo("AAPL");

        assertThat(apple.marketValue())
                .isEqualByComparingTo(
                        "3000.0000000000000000"
                );

        assertThat(apple.allocationPercent())
                .isEqualByComparingTo(
                        "30.000000"
                );

        assertThat(microsoft.symbol())
                .isEqualTo("MSFT");

        assertThat(microsoft.marketValue())
                .isEqualByComparingTo(
                        "2000.0000000000000000"
                );

        assertThat(microsoft.allocationPercent())
                .isEqualByComparingTo(
                        "20.000000"
                );

        assertThat(
                apple.allocationPercent()
                        .add(
                                microsoft.allocationPercent()
                        )
        )
                .isEqualByComparingTo(
                        "50.000000"
                );

        verify(portfolioService)
                .getPortfolio(
                        userId,
                        portfolioId
                );

        verify(portfolioHoldingRepository)
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId
                );

        verifyNoInteractions(
                transactionRepository
        );
    }

    @Test
    void shouldSortHoldingsByMarketValueDescending() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Sorted Portfolio"
                );

        PortfolioHoldingEntity smallHolding =
                createHolding(
                        portfolio,
                        "AAA",
                        "Small Asset",
                        "5.00000000",
                        "100.00000000",
                        "100.00000000",
                        "0.00000000"
                );

        PortfolioHoldingEntity largeHolding =
                createHolding(
                        portfolio,
                        "ZZZ",
                        "Large Asset",
                        "20.00000000",
                        "100.00000000",
                        "200.00000000",
                        "0.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(
                                smallHolding,
                                largeHolding
                        )
                );

        PortfolioAllocationResponse result =
                portfolioAnalyticsService
                        .getPortfolioAllocation(
                                userId,
                                portfolioId
                        );

        assertThat(result.assets())
                .extracting(
                        AssetAllocationItemResponse::symbol
                )
                .containsExactly(
                        "ZZZ",
                        "AAA"
                );
    }

    @Test
    void shouldSupportFractionalShareQuantities() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Fractional Portfolio"
                );

        PortfolioHoldingEntity holding =
                createHolding(
                        portfolio,
                        "NVDA",
                        "NVIDIA Corporation",
                        "0.12500000",
                        "800.00000000",
                        "960.00000000",
                        "25.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(holding)
                );

        PortfolioAllocationResponse result =
                portfolioAnalyticsService
                        .getPortfolioAllocation(
                                userId,
                                portfolioId
                        );

        AssetAllocationItemResponse item =
                result.assets().getFirst();

        assertThat(item.quantity())
                .isEqualByComparingTo(
                        "0.12500000"
                );

        assertThat(item.totalCost())
                .isEqualByComparingTo(
                        "100.0000000000000000"
                );

        assertThat(item.marketValue())
                .isEqualByComparingTo(
                        "120.0000000000000000"
                );

        assertThat(item.unrealizedProfit())
                .isEqualByComparingTo(
                        "20.0000000000000000"
                );

        assertThat(item.allocationPercent())
                .isEqualByComparingTo(
                        "1.200000"
                );

        assertThat(result.cashBalance())
                .isEqualByComparingTo(
                        "9880.0000000000000000"
                );
    }

    @Test
    void shouldExcludeEmptyHoldings() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Filtered Portfolio"
                );

        PortfolioHoldingEntity activeHolding =
                createHolding(
                        portfolio,
                        "AAPL",
                        "Apple Inc.",
                        "2.00000000",
                        "100.00000000",
                        "150.00000000",
                        "0.00000000"
                );

        PortfolioHoldingEntity emptyHolding =
                createHolding(
                        portfolio,
                        "MSFT",
                        "Microsoft Corporation",
                        "0.00000000",
                        "200.00000000",
                        "250.00000000",
                        "0.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(
                                activeHolding,
                                emptyHolding
                        )
                );

        PortfolioAllocationResponse result =
                portfolioAnalyticsService
                        .getPortfolioAllocation(
                                userId,
                                portfolioId
                        );

        assertThat(result.assets())
                .hasSize(1);

        assertThat(result.allocatedAssetCount())
                .isEqualTo(1L);

        assertThat(
                result.assets()
                        .getFirst()
                        .symbol()
        )
                .isEqualTo("AAPL");
    }

    @Test
    void shouldReturnEmptyAllocationForPortfolioWithoutHoldings() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Empty Allocation Portfolio"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId
                ))
                .thenReturn(
                        List.of()
                );

        PortfolioAllocationResponse result =
                portfolioAnalyticsService
                        .getPortfolioAllocation(
                                userId,
                                portfolioId
                        );

        assertThat(result.portfolioValue())
                .isEqualByComparingTo(
                        "10000.0000"
                );

        assertThat(result.cashBalance())
                .isEqualByComparingTo(
                        "10000.0000"
                );

        assertThat(result.totalMarketValue())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalCost())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalRealizedProfit())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalUnrealizedProfit())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.totalTargetWeightPercent())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.allocatedAssetCount())
                .isZero();

        assertThat(result.assets())
                .isEmpty();

        assertThat(result.calculatedAt())
                .isNotNull();
    }

    @Test
    void shouldPreserveNegativeProfitValues() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Loss Portfolio"
                );

        PortfolioHoldingEntity holding =
                createHolding(
                        portfolio,
                        "TSLA",
                        "Tesla Inc.",
                        "10.00000000",
                        "300.00000000",
                        "200.00000000",
                        "-250.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(holding)
                );

        PortfolioAllocationResponse result =
                portfolioAnalyticsService
                        .getPortfolioAllocation(
                                userId,
                                portfolioId
                        );

        AssetAllocationItemResponse item =
                result.assets().getFirst();

        assertThat(item.realizedProfit())
                .isEqualByComparingTo(
                        "-250.00000000"
                );

        assertThat(item.unrealizedProfit())
                .isEqualByComparingTo(
                        "-1000.0000000000000000"
                );

        assertThat(result.totalRealizedProfit())
                .isEqualByComparingTo(
                        "-250.00000000"
                );

        assertThat(result.totalUnrealizedProfit())
                .isEqualByComparingTo(
                        "-1000.0000000000000000"
                );
    }

    @Test
    void shouldCalculateTargetValuesAndBuyAction() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio =
                createPortfolio(
                        portfolioId,
                        "Target Portfolio"
                );

        PortfolioHoldingEntity holding =
                createHolding(
                        portfolio,
                        "AAPL",
                        "Apple Inc.",
                        "10.00000000",
                        "100.00000000",
                        "200.00000000",
                        "0.00000000"
                );

        holding.updateTargetWeightPercent(
                new BigDecimal("30.000000")
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId
                ))
                .thenReturn(
                        List.of(holding)
                );

        PortfolioAllocationResponse result =
                portfolioAnalyticsService
                        .getPortfolioAllocation(
                                userId,
                                portfolioId
                        );

        AssetAllocationItemResponse item =
                result.assets().getFirst();

        assertThat(result.totalTargetWeightPercent())
                .isEqualByComparingTo(
                        "30.000000"
                );

        assertThat(item.targetMarketValue())
                .isEqualByComparingTo(
                        "3000.00000000"
                );

        assertThat(item.targetQuantity())
                .isEqualByComparingTo(
                        "15.00000000"
                );

        assertThat(item.quantityDifference())
                .isEqualByComparingTo(
                        "5.00000000"
                );

        assertThat(item.estimatedTradeValue())
                .isEqualByComparingTo(
                        "1000.00000000"
                );

        assertThat(item.rebalanceAction())
                .isEqualTo("BUY");
    }

    @Test
    void shouldNotQueryHoldingRepositoryWhenPortfolioDoesNotExist() {
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
                        .getPortfolioAllocation(
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
                portfolioHoldingRepository
        );

        verifyNoInteractions(
                transactionRepository
        );
    }

    private PortfolioEntity createPortfolio(
            UUID portfolioId,
            String portfolioName
    ) {
        UserEntity user =
                new UserEntity(
                        "allocation@example.com",
                        "hashed-password",
                        "Allocation",
                        "User"
                );

        PortfolioEntity portfolio =
                new PortfolioEntity(
                        user,
                        portfolioName,
                        PortfolioCreationMethod.BY_AMOUNT,
                        new BigDecimal(
                                "10000.0000"
                        )
                );

        ReflectionTestUtils.setField(
                portfolio,
                "id",
                portfolioId
        );

        return portfolio;
    }

    private PortfolioHoldingEntity createHolding(
            PortfolioEntity portfolio,
            String symbol,
            String displayName,
            String quantity,
            String averageCost,
            String currentPrice,
            String realizedProfit
    ) {
        AssetEntity asset =
                new AssetEntity(
                        portfolio,
                        symbol,
                        displayName,
                        AssetType.STOCK,
                        "USD",
                        null,
                        "NASDAQ",
                        null
                );

        ReflectionTestUtils.setField(
                asset,
                "id",
                UUID.randomUUID()
        );

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
}