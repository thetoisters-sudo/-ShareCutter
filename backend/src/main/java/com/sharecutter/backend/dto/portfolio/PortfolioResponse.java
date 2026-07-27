package com.sharecutter.backend.dto.portfolio;

import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PortfolioResponse(

        UUID id,

        UUID userId,

        String name,

        PortfolioCreationMethod creationMethod,

        BigDecimal initialValue,

        BigDecimal currentValue,

        BigDecimal totalRealizedProfit,

        BigDecimal totalUnrealizedProfit,

        BigDecimal totalReturnPercent,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt

) {
}