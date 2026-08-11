package com.sharecutter.backend.dto.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PortfolioHistoryPointResponse(
        BigDecimal currentValue,
        BigDecimal cashBalance,
        BigDecimal holdingsMarketValue,
        BigDecimal totalProfit,
        BigDecimal totalReturnPercent,
        OffsetDateTime capturedAt
) {
}