package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.dto.marketdata.MarketSymbolSearchResponse;
import com.sharecutter.backend.service.marketdata.MarketDataProvider;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTests {

    @Mock
    private MarketDataProvider
            marketDataProvider;

    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService =
                new MarketDataService(
                        marketDataProvider
                );
    }

    @Test
    void shouldNormalizeAndSearchSymbols() {
        MarketSymbolSearchResponse result =
                new MarketSymbolSearchResponse(
                        "TTWO",
                        "Take-Two Interactive "
                                + "Software Inc.",
                        "NASDAQ",
                        "XNAS",
                        "Common Stock",
                        "United States",
                        "USD",
                        AssetType.STOCK
                );

        when(marketDataProvider.searchSymbols(
                "Take Two",
                10
        )).thenReturn(List.of(result));

        List<MarketSymbolSearchResponse>
                response =
                marketDataService.searchSymbols(
                        "  Take Two  ",
                        null
                );

        assertThat(response)
                .containsExactly(result);

        verify(marketDataProvider)
                .searchSymbols(
                        "Take Two",
                        10
                );
    }

    @Test
    void shouldRespectExplicitSearchLimit() {
        when(marketDataProvider.searchSymbols(
                "NVDA",
                5
        )).thenReturn(List.of());

        marketDataService.searchSymbols(
                "NVDA",
                5
        );

        verify(marketDataProvider)
                .searchSymbols(
                        "NVDA",
                        5
                );
    }

    @Test
    void shouldRejectBlankSearchQuery() {
        assertThatThrownBy(
                () ->
                        marketDataService
                                .searchSymbols(
                                        "   ",
                                        null
                                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "must not be blank"
                );

        verify(
                marketDataProvider,
                never()
        ).searchSymbols(
                org.mockito.ArgumentMatchers
                        .anyString(),
                org.mockito.ArgumentMatchers
                        .anyInt()
        );
    }

    @Test
    void shouldRejectSearchQueryShorterThanTwoCharacters() {
        assertThatThrownBy(
                () ->
                        marketDataService
                                .searchSymbols(
                                        "A",
                                        null
                                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "at least 2 characters"
                );
    }

    @Test
    void shouldRejectInvalidSearchLimit() {
        assertThatThrownBy(
                () ->
                        marketDataService
                                .searchSymbols(
                                        "AAPL",
                                        26
                                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "between 1 and 25"
                );
    }

    @Test
    void shouldNormalizeSymbolAndExchangeForPrice() {
        MarketPriceResponse expected =
                new MarketPriceResponse(
                        "TTWO",
                        "NASDAQ",
                        new BigDecimal(
                                "205.55000000"
                        ),
                        OffsetDateTime.now(
                                ZoneOffset.UTC
                        )
                );

        when(marketDataProvider.getLatestPrice(
                "TTWO",
                "NASDAQ"
        )).thenReturn(expected);

        MarketPriceResponse response =
                marketDataService.getLatestPrice(
                        "  ttwo  ",
                        "  NASDAQ  "
                );

        assertThat(response)
                .isSameAs(expected);

        verify(marketDataProvider)
                .getLatestPrice(
                        "TTWO",
                        "NASDAQ"
                );
    }

    @Test
    void shouldAllowPriceRequestWithoutExchange() {
        MarketPriceResponse expected =
                new MarketPriceResponse(
                        "AAPL",
                        null,
                        new BigDecimal(
                                "200.00000000"
                        ),
                        OffsetDateTime.now(
                                ZoneOffset.UTC
                        )
                );

        when(marketDataProvider.getLatestPrice(
                "AAPL",
                null
        )).thenReturn(expected);

        MarketPriceResponse response =
                marketDataService.getLatestPrice(
                        "aapl",
                        "   "
                );

        assertThat(response)
                .isSameAs(expected);
    }

    @Test
    void shouldRejectBlankPriceSymbol() {
        assertThatThrownBy(
                () ->
                        marketDataService
                                .getLatestPrice(
                                        " ",
                                        null
                                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "must not be blank"
                );
    }
}