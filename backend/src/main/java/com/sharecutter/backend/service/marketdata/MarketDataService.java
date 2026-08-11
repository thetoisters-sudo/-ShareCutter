package com.sharecutter.backend.service.marketdata;

import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.dto.marketdata.MarketSymbolSearchResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class MarketDataService {

    private static final int DEFAULT_SEARCH_LIMIT =
            10;

    private static final int MAX_SEARCH_LIMIT =
            25;

    private final MarketDataProvider
            marketDataProvider;

    public MarketDataService(
            MarketDataProvider marketDataProvider
    ) {
        this.marketDataProvider =
                marketDataProvider;
    }

    public List<MarketSymbolSearchResponse>
    searchSymbols(
            String query,
            Integer limit
    ) {
        String normalizedQuery =
                normalizeSearchQuery(query);

        int normalizedLimit =
                normalizeLimit(limit);

        return marketDataProvider.searchSymbols(
                normalizedQuery,
                normalizedLimit
        );
    }

    public MarketPriceResponse getLatestPrice(
            String symbol,
            String exchange
    ) {
        String normalizedSymbol =
                normalizeSymbol(symbol);

        String normalizedExchange =
                normalizeOptionalExchange(
                        exchange
                );

        return marketDataProvider.getLatestPrice(
                normalizedSymbol,
                normalizedExchange
        );
    }

    private String normalizeSearchQuery(
            String query
    ) {
        if (
                query == null
                        || query.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Market-data search query "
                            + "must not be blank"
            );
        }

        String normalizedQuery =
                query.trim();

        if (normalizedQuery.length() < 2) {
            throw new IllegalArgumentException(
                    "Market-data search query "
                            + "must contain at least 2 characters"
            );
        }

        if (normalizedQuery.length() > 80) {
            throw new IllegalArgumentException(
                    "Market-data search query "
                            + "must not exceed 80 characters"
            );
        }

        return normalizedQuery;
    }

    private int normalizeLimit(
            Integer limit
    ) {
        if (limit == null) {
            return DEFAULT_SEARCH_LIMIT;
        }

        if (
                limit < 1
                        || limit > MAX_SEARCH_LIMIT
        ) {
            throw new IllegalArgumentException(
                    "Market-data search limit "
                            + "must be between 1 and "
                            + MAX_SEARCH_LIMIT
            );
        }

        return limit;
    }

    private String normalizeSymbol(
            String symbol
    ) {
        if (
                symbol == null
                        || symbol.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Market symbol must not be blank"
            );
        }

        String normalizedSymbol =
                symbol
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (normalizedSymbol.length() > 30) {
            throw new IllegalArgumentException(
                    "Market symbol must not exceed "
                            + "30 characters"
            );
        }

        return normalizedSymbol;
    }

    private String normalizeOptionalExchange(
            String exchange
    ) {
        if (
                exchange == null
                        || exchange.isBlank()
        ) {
            return null;
        }

        String normalizedExchange =
                exchange.trim();

        if (normalizedExchange.length() > 40) {
            throw new IllegalArgumentException(
                    "Market exchange must not exceed "
                            + "40 characters"
            );
        }

        return normalizedExchange;
    }
}