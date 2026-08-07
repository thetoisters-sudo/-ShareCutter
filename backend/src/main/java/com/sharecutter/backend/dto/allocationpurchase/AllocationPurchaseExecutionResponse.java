package com.sharecutter.backend.dto.allocationpurchase;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AllocationPurchaseExecutionResponse(

        UUID portfolioId,

        String portfolioName,

        UUID assetId,

        String symbol,

        String displayName,

        UUID transactionId,

        BigDecimal targetWeightPercent,

        BigDecimal unitPrice,

        BigDecimal purchasedQuantity,

        BigDecimal purchaseAmount,

        BigDecimal fee,

        BigDecimal totalCashUsed,

        BigDecimal availableCashBefore,

        BigDecimal availableCashAfter,

        BigDecimal remainingAssignableWeightPercent,

        String currency,

        OffsetDateTime executedAt

) {

    public AllocationPurchaseExecutionResponse {
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

        if (transactionId == null) {
            throw new IllegalArgumentException(
                    "Transaction id must not be null"
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
                "Currency"
        );

        targetWeightPercent =
                zeroIfNull(targetWeightPercent);

        unitPrice =
                zeroIfNull(unitPrice);

        purchasedQuantity =
                zeroIfNull(purchasedQuantity);

        purchaseAmount =
                zeroIfNull(purchaseAmount);

        fee =
                zeroIfNull(fee);

        totalCashUsed =
                zeroIfNull(totalCashUsed);

        availableCashBefore =
                zeroIfNull(availableCashBefore);

        availableCashAfter =
                zeroIfNull(availableCashAfter);

        remainingAssignableWeightPercent =
                zeroIfNull(
                        remainingAssignableWeightPercent
                );

        if (executedAt == null) {
            throw new IllegalArgumentException(
                    "Execution time must not be null"
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