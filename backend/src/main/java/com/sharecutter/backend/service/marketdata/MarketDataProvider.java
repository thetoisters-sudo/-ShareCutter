package com.sharecutter.backend.service.marketdata;

import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.dto.marketdata.MarketSymbolSearchResponse;

import java.util.List;

public interface MarketDataProvider {

    List<MarketSymbolSearchResponse> searchSymbols(
            String query,
            int limit
    );

    MarketPriceResponse getLatestPrice(
            String symbol,
            String exchange
    );
}