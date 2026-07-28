package com.sharecutter.backend.dto.weeklytarget;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WeeklyTargetItemRequest(

        @NotNull(message = "Asset id is required")
        UUID assetId,

        @NotNull(message = "Target percentage is required")
        @DecimalMin(
                value = "0.0000",
                inclusive = true,
                message = "Target percentage must not be negative"
        )
        @DecimalMax(
                value = "100.0000",
                inclusive = true,
                message = "Target percentage must not exceed 100"
        )
        @Digits(
                integer = 3,
                fraction = 4,
                message = "Target percentage must contain "
                        + "up to 3 integer digits and 4 decimal places"
        )
        BigDecimal targetPercentage

) {
}