package com.sharecutter.backend.dto.marketdata;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MarketPriceResponse(

        String symbol,

        String exchange,

        BigDecimal price,

        OffsetDateTime retrievedAt

) {
}