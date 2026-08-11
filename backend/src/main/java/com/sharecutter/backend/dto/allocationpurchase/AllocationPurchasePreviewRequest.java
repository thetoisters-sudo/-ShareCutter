package com.sharecutter.backend.dto.allocationpurchase;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AllocationPurchasePreviewRequest(
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
        BigDecimal targetWeightPercent
) {
}