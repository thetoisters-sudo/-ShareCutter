package com.sharecutter.backend.dto.analytics;

import com.sharecutter.backend.domain.enums.AssetType;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetAllocationItemResponse(

        UUID assetId,

        String symbol,

        String displayName,

        AssetType assetType,

        String currency,

        BigDecimal quantity,

        BigDecimal averageCost,

        BigDecimal currentPrice,

        BigDecimal totalCost,

        BigDecimal marketValue,

        BigDecimal realizedProfit,

        BigDecimal unrealizedProfit,

        BigDecimal allocationPercent,

        BigDecimal targetWeightPercent,

        BigDecimal targetMarketValue,

        BigDecimal targetQuantity,

        BigDecimal quantityDifference,

        BigDecimal estimatedTradeValue,

        String rebalanceAction

) {

    public AssetAllocationItemResponse {
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

        if (assetType == null) {
            throw new IllegalArgumentException(
                    "Asset type must not be null"
            );
        }

        currency = requireText(
                currency,
                "Asset currency"
        );

        quantity = zeroIfNull(
                quantity
        );

        averageCost = zeroIfNull(
                averageCost
        );

        currentPrice = zeroIfNull(
                currentPrice
        );

        totalCost = zeroIfNull(
                totalCost
        );

        marketValue = zeroIfNull(
                marketValue
        );

        realizedProfit = zeroIfNull(
                realizedProfit
        );

        unrealizedProfit = zeroIfNull(
                unrealizedProfit
        );

        allocationPercent = zeroIfNull(
                allocationPercent
        );

        targetWeightPercent = zeroIfNull(
                targetWeightPercent
        );

        targetMarketValue = zeroIfNull(
                targetMarketValue
        );

        targetQuantity = zeroIfNull(
                targetQuantity
        );

        quantityDifference = zeroIfNull(
                quantityDifference
        );

        estimatedTradeValue = zeroIfNull(
                estimatedTradeValue
        );

        rebalanceAction = requireText(
                rebalanceAction,
                "Rebalance action"
        );

        validateNonNegative(
                quantity,
                "Quantity"
        );

        validateNonNegative(
                averageCost,
                "Average cost"
        );

        validateNonNegative(
                currentPrice,
                "Current price"
        );

        validateNonNegative(
                totalCost,
                "Total cost"
        );

        validateNonNegative(
                marketValue,
                "Market value"
        );

        validateNonNegative(
                allocationPercent,
                "Allocation percent"
        );

        validateNonNegative(
                targetWeightPercent,
                "Target weight percent"
        );

        validateNonNegative(
                targetMarketValue,
                "Target market value"
        );

        validateNonNegative(
                targetQuantity,
                "Target quantity"
        );

        validateNonNegative(
                estimatedTradeValue,
                "Estimated trade value"
        );

        if (
                targetWeightPercent.compareTo(
                        BigDecimal.valueOf(100)
                ) > 0
        ) {
            throw new IllegalArgumentException(
                    "Target weight percent must not exceed 100"
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

    private static void validateNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must not be negative"
            );
        }
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must not be blank"
            );
        }

        return value.trim();
    }
}