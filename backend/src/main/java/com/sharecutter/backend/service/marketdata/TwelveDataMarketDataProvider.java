package com.sharecutter.backend.service.marketdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.dto.marketdata.MarketSymbolSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class TwelveDataMarketDataProvider
        implements MarketDataProvider {

    private static final String PROVIDER_NAME =
            "Twelve Data";

    private final RestClient restClient;

    private final String apiKey;

    public TwelveDataMarketDataProvider(
            RestClient.Builder restClientBuilder,
            @Value(
                    "${app.market-data.twelve-data.base-url:"
                            + "https://api.twelvedata.com}"
            )
            String baseUrl,
            @Value(
                    "${app.market-data.twelve-data.api-key:}"
            )
            String apiKey
    ) {
        this.apiKey =
                apiKey == null
                        ? ""
                        : apiKey.trim();

        this.restClient =
                restClientBuilder
                        .baseUrl(baseUrl)
                        .requestFactory(
                                requestFactory()
                        )
                        .build();
    }

    @Override
    public List<MarketSymbolSearchResponse>
    searchSymbols(
            String query,
            int limit
    ) {
        requireConfiguredApiKey();

        TwelveDataSearchResponse response =
                restClient
                        .get()
                        .uri(
                                uriBuilder ->
                                        uriBuilder
                                                .path(
                                                        "/symbol_search"
                                                )
                                                .queryParam(
                                                        "symbol",
                                                        query
                                                )
                                                .queryParam(
                                                        "outputsize",
                                                        limit
                                                )
                                                .build()
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "apikey " + apiKey
                        )
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::
                                        isError,
                                (
                                        request,
                                        providerResponse
                                ) -> {
                                    throw providerFailure(
                                            providerResponse
                                                    .getStatusCode()
                                                    .value()
                                    );
                                }
                        )
                        .body(
                                TwelveDataSearchResponse.class
                        );

        if (response == null) {
            throw providerFailure(502);
        }

        validateProviderStatus(
                response.status(),
                response.message()
        );

        if (response.data() == null) {
            return List.of();
        }

        return response
                .data()
                .stream()
                .filter(Objects::nonNull)
                .map(this::mapSearchResult)
                .toList();
    }

    @Override
    public MarketPriceResponse getLatestPrice(
            String symbol,
            String exchange
    ) {
        requireConfiguredApiKey();

        TwelveDataPriceResponse response =
                restClient
                        .get()
                        .uri(
                                uriBuilder -> {
                                    var builder =
                                            uriBuilder
                                                    .path(
                                                            "/price"
                                                    )
                                                    .queryParam(
                                                            "symbol",
                                                            symbol
                                                    )
                                                    .queryParam(
                                                            "dp",
                                                            8
                                                    );

                                    if (
                                            exchange != null
                                                    && !exchange
                                                    .isBlank()
                                    ) {
                                        builder.queryParam(
                                                "exchange",
                                                exchange
                                        );
                                    }

                                    return builder.build();
                                }
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "apikey " + apiKey
                        )
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::
                                        isError,
                                (
                                        request,
                                        providerResponse
                                ) -> {
                                    throw providerFailure(
                                            providerResponse
                                                    .getStatusCode()
                                                    .value()
                                    );
                                }
                        )
                        .body(
                                TwelveDataPriceResponse.class
                        );

        if (response == null) {
            throw providerFailure(502);
        }

        validateProviderStatus(
                response.status(),
                response.message()
        );

        if (
                response.price() == null
                        || response.price().isBlank()
        ) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus
                            .NOT_FOUND,
                    "A current market price was not returned "
                            + "for symbol " + symbol
            );
        }

        BigDecimal price;

        try {
            price =
                    new BigDecimal(
                            response.price()
                    );
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus
                            .BAD_GATEWAY,
                    PROVIDER_NAME
                            + " returned an invalid price",
                    exception
            );
        }

        return new MarketPriceResponse(
                symbol
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        ),
                normalizeOptionalText(
                        exchange
                ),
                price,
                OffsetDateTime.now(
                        ZoneOffset.UTC
                )
        );
    }

    private MarketSymbolSearchResponse
    mapSearchResult(
            TwelveDataSearchItem item
    ) {
        return new MarketSymbolSearchResponse(
                normalizeRequiredText(
                        item.symbol(),
                        "UNKNOWN"
                ).toUpperCase(
                        Locale.ROOT
                ),
                normalizeRequiredText(
                        item.instrumentName(),
                        item.symbol()
                ),
                normalizeOptionalText(
                        item.exchange()
                ),
                normalizeOptionalText(
                        item.micCode()
                ),
                normalizeOptionalText(
                        item.instrumentType()
                ),
                normalizeOptionalText(
                        item.country()
                ),
                normalizeRequiredText(
                        item.currency(),
                        "USD"
                ).toUpperCase(
                        Locale.ROOT
                ),
                mapAssetType(
                        item.instrumentType()
                )
        );
    }

    private AssetType mapAssetType(
            String instrumentType
    ) {
        if (
                instrumentType == null
                        || instrumentType.isBlank()
        ) {
            return AssetType.OTHER;
        }

        String normalized =
                instrumentType
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                normalized.contains("ETF")
                        || normalized.contains(
                        "EXCHANGE-TRADED"
                )
        ) {
            return AssetType.ETF;
        }

        if (
                normalized.contains("STOCK")
                        || normalized.contains(
                        "EQUITY"
                )
                        || normalized.contains(
                        "COMMON"
                )
                        || normalized.contains(
                        "PREFERRED"
                )
                        || normalized.contains(
                        "REIT"
                )
        ) {
            return AssetType.STOCK;
        }

        if (normalized.contains("BOND")) {
            return AssetType.BOND;
        }

        if (
                normalized.contains("FUND")
                        || normalized.contains(
                        "MUTUAL"
                )
        ) {
            return AssetType.FUND;
        }

        if (
                normalized.contains("CRYPTO")
                        || normalized.contains(
                        "DIGITAL"
                )
        ) {
            return AssetType.CRYPTO;
        }

        if (
                normalized.contains("FOREX")
                        || normalized.contains(
                        "CURRENCY"
                )
        ) {
            return AssetType.FOREX;
        }

        if (
                normalized.contains("COMMODITY")
        ) {
            return AssetType.COMMODITY;
        }

        return AssetType.OTHER;
    }

    private void requireConfiguredApiKey() {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus
                            .SERVICE_UNAVAILABLE,
                    "Market data is not configured. "
                            + "Set TWELVE_DATA_API_KEY."
            );
        }
    }

    private void validateProviderStatus(
            String status,
            String message
    ) {
        if (
                status == null
                        || !"error".equalsIgnoreCase(
                        status
                )
        ) {
            return;
        }

        throw new ResponseStatusException(
                org.springframework.http.HttpStatus
                        .BAD_GATEWAY,
                message == null
                        || message.isBlank()
                        ? PROVIDER_NAME
                        + " rejected the request"
                        : message.trim()
        );
    }

    private ResponseStatusException
    providerFailure(
            int providerStatus
    ) {
        if (providerStatus == 429) {
            return new ResponseStatusException(
                    org.springframework.http.HttpStatus
                            .TOO_MANY_REQUESTS,
                    "The market-data request limit "
                            + "has been reached"
            );
        }

        if (
                providerStatus == 401
                        || providerStatus == 403
        ) {
            return new ResponseStatusException(
                    org.springframework.http.HttpStatus
                            .SERVICE_UNAVAILABLE,
                    "The market-data provider "
                            + "rejected its API key"
            );
        }

        return new ResponseStatusException(
                org.springframework.http.HttpStatus
                        .BAD_GATEWAY,
                PROVIDER_NAME
                        + " could not complete the request"
        );
    }

    private String normalizeRequiredText(
            String value,
            String fallback
    ) {
        String normalized =
                normalizeOptionalText(value);

        if (normalized != null) {
            return normalized;
        }

        return fallback == null
                || fallback.isBlank()
                ? "UNKNOWN"
                : fallback.trim();
    }

    private String normalizeOptionalText(
            String value
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private org.springframework.http.client
    .JdkClientHttpRequestFactory
    requestFactory() {
        java.net.http.HttpClient httpClient =
                java.net.http.HttpClient
                        .newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(5)
                        )
                        .build();

        var requestFactory =
                new org.springframework.http.client
                        .JdkClientHttpRequestFactory(
                        httpClient
                );

        requestFactory.setReadTimeout(
                Duration.ofSeconds(8)
        );

        return requestFactory;
    }

    @JsonIgnoreProperties(
            ignoreUnknown = true
    )
    private record TwelveDataSearchResponse(

            List<TwelveDataSearchItem> data,

            String status,

            String message

    ) {
    }

    @JsonIgnoreProperties(
            ignoreUnknown = true
    )
    private record TwelveDataSearchItem(

            String symbol,

            @JsonProperty("instrument_name")
            String instrumentName,

            String exchange,

            @JsonProperty("mic_code")
            String micCode,

            @JsonProperty("instrument_type")
            String instrumentType,

            String country,

            String currency

    ) {
    }

    @JsonIgnoreProperties(
            ignoreUnknown = true
    )
    private record TwelveDataPriceResponse(

            String price,

            String status,

            String message

    ) {
    }
}

