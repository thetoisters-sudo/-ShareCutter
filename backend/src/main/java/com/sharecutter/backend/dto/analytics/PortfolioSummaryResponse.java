package com.sharecutter.backend.dto.analytics;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PortfolioSummaryResponse(
        UUID portfolioId,
        String portfolioName,
        BigDecimal initialValue,
        BigDecimal currentValue,
        BigDecimal totalRealizedProfit,
        BigDecimal totalUnrealizedProfit,
        BigDecimal totalProfit,
        BigDecimal totalReturnPercent,
        long activeAssetCount,
        long transactionCount,
        BigDecimal totalBuyAmount,
        BigDecimal totalSellAmount,
        BigDecimal totalDividendAmount,
        BigDecimal totalFeeAmount,
        BigDecimal totalDepositAmount,
        BigDecimal totalWithdrawalAmount,
        BigDecimal netCashFlow,
        OffsetDateTime calculatedAt
) {

    public PortfolioSummaryResponse {
        initialValue = zeroIfNull(initialValue);
        currentValue = zeroIfNull(currentValue);
        totalRealizedProfit = zeroIfNull(totalRealizedProfit);
        totalUnrealizedProfit = zeroIfNull(totalUnrealizedProfit);
        totalProfit = zeroIfNull(totalProfit);
        totalReturnPercent = zeroIfNull(totalReturnPercent);
        totalBuyAmount = zeroIfNull(totalBuyAmount);
        totalSellAmount = zeroIfNull(totalSellAmount);
        totalDividendAmount = zeroIfNull(totalDividendAmount);
        totalFeeAmount = zeroIfNull(totalFeeAmount);
        totalDepositAmount = zeroIfNull(totalDepositAmount);
        totalWithdrawalAmount = zeroIfNull(totalWithdrawalAmount);
        netCashFlow = zeroIfNull(netCashFlow);

        if (activeAssetCount < 0) {
            throw new IllegalArgumentException(
                    "Active asset count must not be negative"
            );
        }

        if (transactionCount < 0) {
            throw new IllegalArgumentException(
                    "Transaction count must not be negative"
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
}