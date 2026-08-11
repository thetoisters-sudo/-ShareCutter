package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.dto.marketdata.PortfolioMarketRefreshResponse;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.service.marketdata.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioMarketRefreshServiceTests {

    private static final UUID USER_ID =
            UUID.randomUUID();

    private static final UUID PORTFOLIO_ID =
            UUID.randomUUID();

    private static final UUID SECOND_PORTFOLIO_ID =
            UUID.randomUUID();

    @Mock
    private PortfolioService
            portfolioService;

    @Mock
    private PortfolioHoldingRepository
            portfolioHoldingRepository;

    @Mock
    private PortfolioHoldingCalculationService
            holdingCalculationService;

    @Mock
    private MarketDataService
            marketDataService;

    @Mock
    private PortfolioEntity
            portfolio;

    @Mock
    private PortfolioEntity
            secondPortfolio;

    @Mock
    private AssetEntity
            appleAsset;

    @Mock
    private AssetEntity
            secondAppleAsset;

    @Mock
    private AssetEntity
            takeTwoAsset;

    private PortfolioMarketRefreshService
            refreshService;

    @BeforeEach
    void setUp() {
        refreshService =
                new PortfolioMarketRefreshService(
                        portfolioService,
                        portfolioHoldingRepository,
                        holdingCalculationService,
                        marketDataService
                );
    }

    @Test
    void shouldRefreshActiveHoldingsAndRecalculatePortfolio() {
        when(portfolio.getId())
                .thenReturn(
                        PORTFOLIO_ID
                );

        when(appleAsset.getSymbol())
                .thenReturn(
                        "AAPL"
                );

        when(appleAsset.getExchange())
                .thenReturn(
                        "NASDAQ"
                );

        when(takeTwoAsset.getSymbol())
                .thenReturn(
                        "TTWO"
                );

        when(takeTwoAsset.getExchange())
                .thenReturn(
                        "NASDAQ"
                );

        PortfolioHoldingEntity appleHolding =
                holding(
                        portfolio,
                        appleAsset,
                        "5.00000000",
                        "180.00000000",
                        "180.00000000"
                );

        PortfolioHoldingEntity takeTwoHolding =
                holding(
                        portfolio,
                        takeTwoAsset,
                        "2.00000000",
                        "210.00000000",
                        "210.00000000"
                );

        when(portfolioService
                .getUserPortfolios(
                        USER_ID
                ))
                .thenReturn(
                        List.of(
                                portfolio
                        )
                );

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(
                                appleHolding,
                                takeTwoHolding
                        )
                );

        when(marketDataService
                .getLatestPrice(
                        "AAPL",
                        "NASDAQ"
                ))
                .thenReturn(
                        marketPrice(
                                "AAPL",
                                "NASDAQ",
                                "200.00000000"
                        )
                );

        when(marketDataService
                .getLatestPrice(
                        "TTWO",
                        "NASDAQ"
                ))
                .thenReturn(
                        marketPrice(
                                "TTWO",
                                "NASDAQ",
                                "250.00000000"
                        )
                );

        PortfolioMarketRefreshResponse response =
                refreshService
                        .refreshUserPortfolios(
                                USER_ID
                        );

        assertThat(
                appleHolding.getCurrentPrice()
        )
                .isEqualByComparingTo(
                        "200.00000000"
                );

        assertThat(
                appleHolding.getMarketValue()
        )
                .isEqualByComparingTo(
                        "1000.00000000"
                );

        assertThat(
                takeTwoHolding.getCurrentPrice()
        )
                .isEqualByComparingTo(
                        "250.00000000"
                );

        assertThat(
                takeTwoHolding.getMarketValue()
        )
                .isEqualByComparingTo(
                        "500.00000000"
                );

        verify(portfolioHoldingRepository)
                .saveAll(
                        anyList()
                );

        verify(holdingCalculationService)
                .recalculatePortfolio(
                        USER_ID,
                        PORTFOLIO_ID
                );

        assertThat(
                response.portfolioCount()
        ).isEqualTo(1);

        assertThat(
                response.refreshedPortfolioCount()
        ).isEqualTo(1);

        assertThat(
                response.refreshedHoldingCount()
        ).isEqualTo(2);

        assertThat(
                response.failedSymbols()
        ).isEmpty();

        assertThat(
                response.refreshedAt()
        ).isNotNull();
    }

    @Test
    void shouldRequestSameInstrumentOnlyOnceAcrossPortfolios() {
        when(portfolio.getId())
                .thenReturn(
                        PORTFOLIO_ID
                );

        when(secondPortfolio.getId())
                .thenReturn(
                        SECOND_PORTFOLIO_ID
                );

        when(appleAsset.getSymbol())
                .thenReturn(
                        "AAPL"
                );

        when(appleAsset.getExchange())
                .thenReturn(
                        "NASDAQ"
                );

        when(secondAppleAsset.getSymbol())
                .thenReturn(
                        "aapl"
                );

        when(secondAppleAsset.getExchange())
                .thenReturn(
                        "nasdaq"
                );

        PortfolioHoldingEntity firstHolding =
                holding(
                        portfolio,
                        appleAsset,
                        "5.00000000",
                        "180.00000000",
                        "180.00000000"
                );

        PortfolioHoldingEntity secondHolding =
                holding(
                        secondPortfolio,
                        secondAppleAsset,
                        "3.00000000",
                        "190.00000000",
                        "190.00000000"
                );

        when(portfolioService
                .getUserPortfolios(
                        USER_ID
                ))
                .thenReturn(
                        List.of(
                                portfolio,
                                secondPortfolio
                        )
                );

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(
                                firstHolding
                        )
                );

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        SECOND_PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(
                                secondHolding
                        )
                );

        when(marketDataService
                .getLatestPrice(
                        "AAPL",
                        "NASDAQ"
                ))
                .thenReturn(
                        marketPrice(
                                "AAPL",
                                "NASDAQ",
                                "205.50000000"
                        )
                );

        PortfolioMarketRefreshResponse response =
                refreshService
                        .refreshUserPortfolios(
                                USER_ID
                        );

        verify(marketDataService)
                .getLatestPrice(
                        "AAPL",
                        "NASDAQ"
                );

        assertThat(
                firstHolding.getCurrentPrice()
        )
                .isEqualByComparingTo(
                        "205.50000000"
                );

        assertThat(
                secondHolding.getCurrentPrice()
        )
                .isEqualByComparingTo(
                        "205.50000000"
                );

        verify(holdingCalculationService)
                .recalculatePortfolio(
                        USER_ID,
                        PORTFOLIO_ID
                );

        verify(holdingCalculationService)
                .recalculatePortfolio(
                        USER_ID,
                        SECOND_PORTFOLIO_ID
                );

        assertThat(
                response.refreshedPortfolioCount()
        ).isEqualTo(2);

        assertThat(
                response.refreshedHoldingCount()
        ).isEqualTo(2);
    }

    @Test
    void shouldKeepOldPriceWhenOneInstrumentRefreshFails() {
        when(portfolio.getId())
                .thenReturn(
                        PORTFOLIO_ID
                );

        when(appleAsset.getSymbol())
                .thenReturn(
                        "AAPL"
                );

        when(appleAsset.getExchange())
                .thenReturn(
                        "NASDAQ"
                );

        when(takeTwoAsset.getSymbol())
                .thenReturn(
                        "TTWO"
                );

        when(takeTwoAsset.getExchange())
                .thenReturn(
                        "NASDAQ"
                );

        PortfolioHoldingEntity appleHolding =
                holding(
                        portfolio,
                        appleAsset,
                        "5.00000000",
                        "180.00000000",
                        "180.00000000"
                );

        PortfolioHoldingEntity takeTwoHolding =
                holding(
                        portfolio,
                        takeTwoAsset,
                        "2.00000000",
                        "210.00000000",
                        "210.00000000"
                );

        when(portfolioService
                .getUserPortfolios(
                        USER_ID
                ))
                .thenReturn(
                        List.of(
                                portfolio
                        )
                );

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(
                                appleHolding,
                                takeTwoHolding
                        )
                );

        when(marketDataService
                .getLatestPrice(
                        "AAPL",
                        "NASDAQ"
                ))
                .thenReturn(
                        marketPrice(
                                "AAPL",
                                "NASDAQ",
                                "200.00000000"
                        )
                );

        when(marketDataService
                .getLatestPrice(
                        "TTWO",
                        "NASDAQ"
                ))
                .thenThrow(
                        new IllegalStateException(
                                "Market provider unavailable"
                        )
                );

        PortfolioMarketRefreshResponse response =
                refreshService
                        .refreshUserPortfolios(
                                USER_ID
                        );

        assertThat(
                appleHolding.getCurrentPrice()
        )
                .isEqualByComparingTo(
                        "200.00000000"
                );

        assertThat(
                takeTwoHolding.getCurrentPrice()
        )
                .isEqualByComparingTo(
                        "210.00000000"
                );

        assertThat(
                response.refreshedHoldingCount()
        ).isEqualTo(1);

        assertThat(
                response.failedSymbols()
        )
                .containsExactly(
                        "TTWO@NASDAQ"
                );

        verify(holdingCalculationService)
                .recalculatePortfolio(
                        USER_ID,
                        PORTFOLIO_ID
                );
    }

    @Test
    void shouldSkipEmptyHoldings() {
        when(portfolio.getId())
                .thenReturn(
                        PORTFOLIO_ID
                );

        PortfolioHoldingEntity emptyHolding =
                holding(
                        portfolio,
                        appleAsset,
                        "0.00000000",
                        "180.00000000",
                        "180.00000000"
                );

        when(portfolioService
                .getUserPortfolios(
                        USER_ID
                ))
                .thenReturn(
                        List.of(
                                portfolio
                        )
                );

        when(portfolioHoldingRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        PORTFOLIO_ID
                ))
                .thenReturn(
                        List.of(
                                emptyHolding
                        )
                );

        PortfolioMarketRefreshResponse response =
                refreshService
                        .refreshUserPortfolios(
                                USER_ID
                        );

        verify(
                marketDataService,
                never()
        )
                .getLatestPrice(
                        "AAPL",
                        "NASDAQ"
                );

        verify(
                portfolioHoldingRepository,
                never()
        )
                .saveAll(
                        anyList()
                );

        verify(
                holdingCalculationService,
                never()
        )
                .recalculatePortfolio(
                        USER_ID,
                        PORTFOLIO_ID
                );

        assertThat(
                response.refreshedHoldingCount()
        ).isZero();

        assertThat(
                response.refreshedPortfolioCount()
        ).isZero();
    }

    @Test
    void shouldReturnEmptyResultWhenUserHasNoPortfolios() {
        when(portfolioService
                .getUserPortfolios(
                        USER_ID
                ))
                .thenReturn(
                        List.of()
                );

        PortfolioMarketRefreshResponse response =
                refreshService
                        .refreshUserPortfolios(
                                USER_ID
                        );

        assertThat(
                response.portfolioCount()
        ).isZero();

        assertThat(
                response.refreshedPortfolioCount()
        ).isZero();

        assertThat(
                response.refreshedHoldingCount()
        ).isZero();

        assertThat(
                response.failedSymbols()
        ).isEmpty();

        verify(
                portfolioHoldingRepository,
                never()
        )
                .saveAll(
                        anyList()
                );
    }

    private PortfolioHoldingEntity holding(
            PortfolioEntity holdingPortfolio,
            AssetEntity asset,
            String quantity,
            String averageCost,
            String currentPrice
    ) {
        PortfolioHoldingEntity holding =
                new PortfolioHoldingEntity(
                        holdingPortfolio,
                        asset
                );

        holding.replaceCalculatedState(
                new BigDecimal(
                        quantity
                ),
                new BigDecimal(
                        averageCost
                ),
                new BigDecimal(
                        currentPrice
                ),
                BigDecimal.ZERO
        );

        return holding;
    }

    private MarketPriceResponse marketPrice(
            String symbol,
            String exchange,
            String price
    ) {
        return new MarketPriceResponse(
                symbol,
                exchange,
                new BigDecimal(
                        price
                ),
                OffsetDateTime.now(
                        ZoneOffset.UTC
                )
        );
    }
}