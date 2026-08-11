package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.dto.marketdata.PortfolioMarketRefreshResponse;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.service.marketdata.MarketDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PortfolioMarketRefreshService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private final PortfolioService
            portfolioService;

    private final PortfolioHoldingRepository
            portfolioHoldingRepository;

    private final PortfolioHoldingCalculationService
            holdingCalculationService;

    private final MarketDataService
            marketDataService;

    public PortfolioMarketRefreshService(
            PortfolioService portfolioService,
            PortfolioHoldingRepository portfolioHoldingRepository,
            PortfolioHoldingCalculationService holdingCalculationService,
            MarketDataService marketDataService
    ) {
        this.portfolioService =
                portfolioService;

        this.portfolioHoldingRepository =
                portfolioHoldingRepository;

        this.holdingCalculationService =
                holdingCalculationService;

        this.marketDataService =
                marketDataService;
    }

    @Transactional
    public PortfolioMarketRefreshResponse
    refreshUserPortfolios(
            UUID userId
    ) {
        List<PortfolioEntity> portfolios =
                portfolioService
                        .getUserPortfolios(
                                userId
                        );

        if (portfolios.isEmpty()) {
            return new PortfolioMarketRefreshResponse(
                    0,
                    0,
                    0,
                    List.of(),
                    OffsetDateTime.now(
                            ZoneOffset.UTC
                    )
            );
        }

        Map<MarketInstrumentKey,
                List<PortfolioHoldingEntity>>
                holdingsByInstrument =
                new LinkedHashMap<>();

        for (
                PortfolioEntity portfolio
                : portfolios
        ) {
            List<PortfolioHoldingEntity> holdings =
                    portfolioHoldingRepository
                            .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                    portfolio.getId()
                            );

            for (
                    PortfolioHoldingEntity holding
                    : holdings
            ) {
                if (
                        !isRefreshableHolding(
                                holding
                        )
                ) {
                    continue;
                }

                AssetEntity asset =
                        holding.getAsset();

                MarketInstrumentKey key =
                        MarketInstrumentKey.from(
                                asset
                        );

                holdingsByInstrument
                        .computeIfAbsent(
                                key,
                                ignored ->
                                        new ArrayList<>()
                        )
                        .add(
                                holding
                        );
            }
        }

        List<PortfolioHoldingEntity>
                updatedHoldings =
                new ArrayList<>();

        Set<UUID>
                affectedPortfolioIds =
                new LinkedHashSet<>();

        List<String>
                failedSymbols =
                new ArrayList<>();

        for (
                Map.Entry<
                        MarketInstrumentKey,
                        List<PortfolioHoldingEntity>>
                        entry
                : holdingsByInstrument
                        .entrySet()
        ) {
            MarketInstrumentKey key =
                    entry.getKey();

            try {
                MarketPriceResponse response =
                        marketDataService
                                .getLatestPrice(
                                        key.symbol(),
                                        key.exchange()
                                );

                BigDecimal latestPrice =
                        requirePositivePrice(
                                response,
                                key
                        );

                for (
                        PortfolioHoldingEntity holding
                        : entry.getValue()
                ) {
                    holding.updateCurrentPrice(
                            latestPrice
                    );

                    updatedHoldings.add(
                            holding
                    );

                    PortfolioEntity portfolio =
                            holding.getPortfolio();

                    if (
                            portfolio != null
                                    && portfolio.getId()
                                    != null
                    ) {
                        affectedPortfolioIds.add(
                                portfolio.getId()
                        );
                    }
                }
            } catch (
                    RuntimeException exception
            ) {
                failedSymbols.add(
                        key.displayValue()
                );
            }
        }

        if (!updatedHoldings.isEmpty()) {
            portfolioHoldingRepository.saveAll(
                    updatedHoldings
            );
        }

        for (
                UUID portfolioId
                : affectedPortfolioIds
        ) {
            holdingCalculationService
                    .recalculatePortfolio(
                            userId,
                            portfolioId
                    );
        }

        return new PortfolioMarketRefreshResponse(
                portfolios.size(),
                affectedPortfolioIds.size(),
                updatedHoldings.size(),
                List.copyOf(
                        failedSymbols
                ),
                OffsetDateTime.now(
                        ZoneOffset.UTC
                )
        );
    }

    private boolean isRefreshableHolding(
            PortfolioHoldingEntity holding
    ) {
        if (
                holding == null
                        || holding.isDeleted()
        ) {
            return false;
        }

        BigDecimal quantity =
                holding.getQuantity();

        if (
                quantity == null
                        || quantity.compareTo(
                                ZERO
                        ) <= 0
        ) {
            return false;
        }

        AssetEntity asset =
                holding.getAsset();

        return asset != null
                && !asset.isDeleted()
                && asset.getSymbol() != null
                && !asset.getSymbol().isBlank();
    }

    private BigDecimal requirePositivePrice(
            MarketPriceResponse response,
            MarketInstrumentKey key
    ) {
        if (
                response == null
                        || response.price() == null
                        || response.price()
                        .compareTo(
                                ZERO
                        ) <= 0
        ) {
            throw new IllegalArgumentException(
                    "Market price for "
                            + key.displayValue()
                            + " must be greater than zero"
            );
        }

        return response.price();
    }

    private record MarketInstrumentKey(
            String symbol,
            String exchange
    ) {

        private static MarketInstrumentKey from(
                AssetEntity asset
        ) {
            String normalizedSymbol =
                    asset.getSymbol()
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );

            String normalizedExchange =
                    asset.getExchange();

            if (
                    normalizedExchange != null
                            && !normalizedExchange
                            .isBlank()
            ) {
                normalizedExchange =
                        normalizedExchange
                                .trim()
                                .toUpperCase(
                                        Locale.ROOT
                                );
            } else {
                normalizedExchange =
                        null;
            }

            return new MarketInstrumentKey(
                    normalizedSymbol,
                    normalizedExchange
            );
        }

        private String displayValue() {
            if (
                    exchange == null
                            || exchange.isBlank()
            ) {
                return symbol;
            }

            return symbol
                    + "@"
                    + exchange;
        }
    }
}