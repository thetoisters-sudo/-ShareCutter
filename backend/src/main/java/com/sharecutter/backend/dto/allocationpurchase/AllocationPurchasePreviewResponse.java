package com.sharecutter.backend.dto.allocationpurchase;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AllocationPurchasePreviewResponse(

        UUID portfolioId,

        String portfolioName,

        UUID assetId,

        String symbol,

        String displayName,

        String exchange,

        String currency,

        BigDecimal portfolioInitialValue,

        BigDecimal currentPrice,

        BigDecimal targetWeightPercent,

        BigDecimal currentlyAssignedWeightPercent,

        BigDecimal remainingAssignableWeightPercent,

        BigDecimal targetMarketValue,

        BigDecimal existingQuantity,

        BigDecimal targetQuantity,

        BigDecimal quantityToBuy,

        BigDecimal estimatedPurchaseAmount,

        String suggestedAction,

        OffsetDateTime calculatedAt

) {

    public AllocationPurchasePreviewResponse {
        if (portfolioId == null) {
            throw new IllegalArgumentException(
                    "Portfolio id must not be null"
            );
        }

        if (assetId == null) {
            throw new IllegalArgumentException(
                    "Asset id must not be null"
            );
        }

        portfolioName = requireText(
                portfolioName,
                "Portfolio name"
        );

        symbol = requireText(
                symbol,
                "Asset symbol"
        );

        displayName = requireText(
                displayName,
                "Asset display name"
        );

        currency = requireText(
                currency,
                "Asset currency"
        );

        suggestedAction = requireText(
                suggestedAction,
                "Suggested action"
        );

        portfolioInitialValue =
                zeroIfNull(
                        portfolioInitialValue
                );

        currentPrice =
                zeroIfNull(
                        currentPrice
                );

        targetWeightPercent =
                zeroIfNull(
                        targetWeightPercent
                );

        currentlyAssignedWeightPercent =
                zeroIfNull(
                        currentlyAssignedWeightPercent
                );

        remainingAssignableWeightPercent =
                zeroIfNull(
                        remainingAssignableWeightPercent
                );

        targetMarketValue =
                zeroIfNull(
                        targetMarketValue
                );

        existingQuantity =
                zeroIfNull(
                        existingQuantity
                );

        targetQuantity =
                zeroIfNull(
                        targetQuantity
                );

        quantityToBuy =
                zeroIfNull(
                        quantityToBuy
                );

        estimatedPurchaseAmount =
                zeroIfNull(
                        estimatedPurchaseAmount
                );
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
        if (
                value == null
                        || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must not be blank"
            );
        }

        return value.trim();
    }
}