package com.sharecutter.backend.dto.analytics;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PortfolioAllocationResponse(

        UUID portfolioId,

        String portfolioName,

        BigDecimal totalNetInvestedAmount,

        long allocatedAssetCount,

        List<AssetAllocationItemResponse> assets,

        OffsetDateTime calculatedAt

) {

    public PortfolioAllocationResponse {
        if (portfolioId == null) {
            throw new IllegalArgumentException(
                    "Portfolio id must not be null"
            );
        }

        portfolioName = requireText(
                portfolioName,
                "Portfolio name"
        );

        totalNetInvestedAmount =
                zeroIfNull(totalNetInvestedAmount);

        if (allocatedAssetCount < 0) {
            throw new IllegalArgumentException(
                    "Allocated asset count must not be negative"
            );
        }

        assets = assets == null
                ? List.of()
                : List.copyOf(assets);

        if (allocatedAssetCount != assets.size()) {
            throw new IllegalArgumentException(
                    "Allocated asset count must match asset list size"
            );
        }

        if (calculatedAt == null) {
            throw new IllegalArgumentException(
                    "Calculation time must not be null"
            );
        }
    }

    private static BigDecimal zeroIfNull(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }
}