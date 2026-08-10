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
        String action,
        BigDecimal targetWeightPercent,
        BigDecimal unitPrice,
        BigDecimal tradedQuantity,
        BigDecimal tradeAmount,
        BigDecimal fee,
        BigDecimal cashImpact,
        BigDecimal availableCashBefore,
        BigDecimal availableCashAfter,
        BigDecimal totalTargetWeightPercent,
        BigDecimal allocationDifferencePercent,
        String currency,
        OffsetDateTime executedAt
) {
    public AllocationPurchaseExecutionResponse {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio id must not be null");
        }
        if (assetId == null) {
            throw new IllegalArgumentException("Asset id must not be null");
        }
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction id must not be null");
        }

        portfolioName = requireText(portfolioName, "Portfolio name");
        symbol = requireText(symbol, "Asset symbol");
        displayName = requireText(displayName, "Asset display name");
        action = requireText(action, "Trade action");
        currency = requireText(currency, "Currency");

        targetWeightPercent = zeroIfNull(targetWeightPercent);
        unitPrice = zeroIfNull(unitPrice);
        tradedQuantity = zeroIfNull(tradedQuantity);
        tradeAmount = zeroIfNull(tradeAmount);
        fee = zeroIfNull(fee);
        cashImpact = zeroIfNull(cashImpact);
        availableCashBefore = zeroIfNull(availableCashBefore);
        availableCashAfter = zeroIfNull(availableCashAfter);
        totalTargetWeightPercent = zeroIfNull(totalTargetWeightPercent);
        allocationDifferencePercent = zeroIfNull(allocationDifferencePercent);

        if (executedAt == null) {
            throw new IllegalArgumentException("Execution time must not be null");
        }
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}