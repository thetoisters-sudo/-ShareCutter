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

        BigDecimal boughtQuantity,

        BigDecimal soldQuantity,

        BigDecimal currentQuantity,

        BigDecimal totalBuyAmount,

        BigDecimal totalSellAmount,

        BigDecimal netInvestedAmount,

        BigDecimal allocationPercent

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

        boughtQuantity =
                zeroIfNull(boughtQuantity);

        soldQuantity =
                zeroIfNull(soldQuantity);

        currentQuantity =
                zeroIfNull(currentQuantity);

        totalBuyAmount =
                zeroIfNull(totalBuyAmount);

        totalSellAmount =
                zeroIfNull(totalSellAmount);

        netInvestedAmount =
                zeroIfNull(netInvestedAmount);

        allocationPercent =
                zeroIfNull(allocationPercent);

        validateNonNegative(
                boughtQuantity,
                "Bought quantity"
        );

        validateNonNegative(
                soldQuantity,
                "Sold quantity"
        );

        validateNonNegative(
                totalBuyAmount,
                "Total buy amount"
        );

        validateNonNegative(
                totalSellAmount,
                "Total sell amount"
        );

        validateNonNegative(
                allocationPercent,
                "Allocation percent"
        );
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
                    fieldName + " must not be negative"
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