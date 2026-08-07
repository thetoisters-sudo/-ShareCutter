package com.sharecutter.backend.dto.marketdata;

import java.time.OffsetDateTime;
import java.util.List;

public record PortfolioMarketRefreshResponse(

        int portfolioCount,

        int refreshedPortfolioCount,

        int refreshedHoldingCount,

        List<String> failedSymbols,

        OffsetDateTime refreshedAt

) {
}