package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.dto.portfolio.PortfolioCreateFromHoldingsRequest;
import com.sharecutter.backend.service.marketdata.MarketDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PortfolioHoldingsCreationService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final int MONEY_SCALE = 8;

    private static final int PORTFOLIO_VALUE_SCALE =
            4;

    private final PortfolioService
            portfolioService;

    private final AssetService
            assetService;

    private final TransactionService
            transactionService;

    private final MarketDataService
            marketDataService;

    public PortfolioHoldingsCreationService(
            PortfolioService portfolioService,
            AssetService assetService,
            TransactionService transactionService,
            MarketDataService marketDataService
    ) {
        this.portfolioService =
                portfolioService;

        this.assetService =
                assetService;

        this.transactionService =
                transactionService;

        this.marketDataService =
                marketDataService;
    }

    @Transactional
    public PortfolioEntity createPortfolioFromHoldings(
            UUID userId,
            PortfolioCreateFromHoldingsRequest request
    ) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "User id must not be null"
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Portfolio holdings request must not be null"
            );
        }

        List<
                PortfolioCreateFromHoldingsRequest
                        .HoldingRequest
                > holdings =
                request.holdings();

        if (holdings == null) {
            throw new IllegalArgumentException(
                    "Holdings list must not be null"
            );
        }

        BigDecimal initialCash =
                normalizeInitialCash(
                        request.initialCash()
                );

        if (
                holdings.isEmpty() &&
                initialCash.compareTo(
                        ZERO
                ) <= 0
        ) {
            throw new IllegalArgumentException(
                    "Add at least one holding or "
                            + "a positive initial cash balance"
            );
        }

        validateUniqueSymbols(
                holdings
        );

        List<ResolvedHolding>
                resolvedHoldings =
                resolveHoldings(
                        holdings
                );

        BigDecimal holdingsValue =
                calculateHoldingsValue(
                        resolvedHoldings
                );

        BigDecimal initialValue =
                holdingsValue.add(
                        initialCash
                );

        if (
                initialValue.compareTo(
                        ZERO
                ) <= 0
        ) {
            throw new IllegalArgumentException(
                    "Calculated initial portfolio value "
                            + "must be greater than zero"
            );
        }

        PortfolioEntity portfolio =
                portfolioService.createPortfolio(
                        userId,
                        request.name(),
                        PortfolioCreationMethod
                                .BY_HOLDINGS,
                        initialValue.setScale(
                                PORTFOLIO_VALUE_SCALE,
                                RoundingMode.HALF_UP
                        )
                );

        OffsetDateTime executedAt =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        if (
                initialCash.compareTo(
                        ZERO
                ) > 0
        ) {
            transactionService
                    .createTransaction(
                            userId,
                            portfolio.getId(),
                            null,
                            TransactionType.DEPOSIT,
                            null,
                            null,
                            ZERO,
                            initialCash,
                            "USD",
                            executedAt,
                            "Initial cash balance imported "
                                    + "during portfolio creation"
                    );
        }

        for (
                ResolvedHolding holding
                : resolvedHoldings
        ) {
            AssetEntity asset =
                    assetService.createAsset(
                            userId,
                            portfolio.getId(),
                            holding.request()
                                    .symbol(),
                            holding.request()
                                    .displayName(),
                            holding.request()
                                    .assetType(),
                            holding.request()
                                    .currency(),
                            null,
                            holding.request()
                                    .exchange(),
                            "Imported during portfolio creation"
                    );

            transactionService
                    .createTransaction(
                            userId,
                            portfolio.getId(),
                            asset.getId(),
                            TransactionType.BUY,
                            holding.request()
                                    .quantity(),
                            holding.price(),
                            ZERO,
                            holding.marketValue(),
                            holding.request()
                                    .currency(),
                            executedAt,
                            "Initial holding imported "
                                    + "during portfolio creation"
                    );
        }

        return portfolioService.getPortfolio(
                userId,
                portfolio.getId()
        );
    }

    private List<ResolvedHolding>
    resolveHoldings(
            List<
                    PortfolioCreateFromHoldingsRequest
                            .HoldingRequest
                    > holdings
    ) {
        List<ResolvedHolding>
                resolvedHoldings =
                new ArrayList<>();

        for (
                PortfolioCreateFromHoldingsRequest
                        .HoldingRequest holding
                : holdings
        ) {
            MarketPriceResponse marketPrice =
                    marketDataService
                            .getLatestPrice(
                                    holding.symbol(),
                                    holding.exchange()
                            );

            BigDecimal price =
                    requirePositivePrice(
                            holding.symbol(),
                            marketPrice.price()
                    );

            BigDecimal marketValue =
                    holding.quantity()
                            .multiply(
                                    price
                            )
                            .setScale(
                                    MONEY_SCALE,
                                    RoundingMode.HALF_UP
                            );

            resolvedHoldings.add(
                    new ResolvedHolding(
                            holding,
                            price,
                            marketValue
                    )
            );
        }

        return resolvedHoldings;
    }

    private BigDecimal calculateHoldingsValue(
            List<ResolvedHolding>
                    resolvedHoldings
    ) {
        BigDecimal total =
                ZERO;

        for (
                ResolvedHolding holding
                : resolvedHoldings
        ) {
            total =
                    total.add(
                            holding.marketValue()
                    );
        }

        return total.setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal normalizeInitialCash(
            BigDecimal initialCash
    ) {
        if (initialCash == null) {
            throw new IllegalArgumentException(
                    "Initial cash must not be null"
            );
        }

        if (
                initialCash.compareTo(
                        ZERO
                ) < 0
        ) {
            throw new IllegalArgumentException(
                    "Initial cash must not be negative"
            );
        }

        return initialCash.setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private void validateUniqueSymbols(
            List<
                    PortfolioCreateFromHoldingsRequest
                            .HoldingRequest
                    > holdings
    ) {
        Set<String> symbols =
                new HashSet<>();

        for (
                PortfolioCreateFromHoldingsRequest
                        .HoldingRequest holding
                : holdings
        ) {
            String normalizedSymbol =
                    holding.symbol()
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );

            String normalizedExchange =
                    holding.exchange() == null
                            ? ""
                            : holding.exchange()
                                    .trim()
                                    .toUpperCase(
                                            Locale.ROOT
                                    );

            String assetKey =
                    normalizedSymbol
                            + "|"
                            + normalizedExchange;

            if (
                    !symbols.add(
                            assetKey
                    )
            ) {
                throw new IllegalArgumentException(
                        "Duplicate holding: "
                                + normalizedSymbol
                                + (
                                normalizedExchange.isBlank()
                                        ? ""
                                        : " on "
                                        + normalizedExchange
                        )
                );
            }
        }
    }

    private BigDecimal requirePositivePrice(
            String symbol,
            BigDecimal price
    ) {
        if (
                price == null ||
                price.compareTo(
                        ZERO
                ) <= 0
        ) {
            throw new IllegalArgumentException(
                    "No positive market price is available for "
                            + symbol
            );
        }

        return price.setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private record ResolvedHolding(

            PortfolioCreateFromHoldingsRequest
                    .HoldingRequest request,

            BigDecimal price,

            BigDecimal marketValue

    ) {
    }
}