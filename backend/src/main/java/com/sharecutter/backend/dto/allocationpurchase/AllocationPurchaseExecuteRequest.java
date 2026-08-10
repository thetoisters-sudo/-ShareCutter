package com.sharecutter.backend.dto.allocationpurchase;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record AllocationPurchaseExecuteRequest(
        @NotNull(message = "Asset id must not be null")
        UUID assetId,

        @NotNull(message = "Target weight percent must not be null")
        @DecimalMin(
                value = "0",
                inclusive = true,
                message = "Target weight percent must not be negative"
        )
        @DecimalMax(
                value = "100",
                inclusive = true,
                message = "Target weight percent must not exceed 100"
        )
        @Digits(
                integer = 3,
                fraction = 6,
                message = "Target weight percent must contain up to 3 integer digits and 6 decimal digits"
        )
        BigDecimal targetWeightPercent,

        @PositiveOrZero(message = "Trade fee must not be negative")
        @Digits(
                integer = 16,
                fraction = 8,
                message = "Trade fee must contain up to 16 integer digits and 8 decimal digits"
        )
        BigDecimal fee
) {
}