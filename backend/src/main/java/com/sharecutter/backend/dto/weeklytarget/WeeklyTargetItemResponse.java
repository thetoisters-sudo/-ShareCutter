package com.sharecutter.backend.dto.weeklytarget;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WeeklyTargetItemResponse(

        UUID id,

        UUID assetId,

        String symbol,

        String displayName,

        BigDecimal targetPercentage,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt

) {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    public WeeklyTargetItemResponse {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Weekly target id must not be null"
            );
        }

        if (assetId == null) {
            throw new IllegalArgumentException(
                    "Asset id must not be null"
            );
        }

        symbol = requireText(
                symbol,
                "Asset symbol"
        );

        displayName = requireText(
                displayName,
                "Asset display name"
        );

        if (targetPercentage == null) {
            throw new IllegalArgumentException(
                    "Target percentage must not be null"
            );
        }

        if (targetPercentage.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Target percentage must not be negative"
            );
        }

        if (targetPercentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(
                    "Target percentage must not exceed 100"
            );
        }

        if (createdAt == null) {
            throw new IllegalArgumentException(
                    "Creation time must not be null"
            );
        }

        if (updatedAt == null) {
            throw new IllegalArgumentException(
                    "Update time must not be null"
            );
        }
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