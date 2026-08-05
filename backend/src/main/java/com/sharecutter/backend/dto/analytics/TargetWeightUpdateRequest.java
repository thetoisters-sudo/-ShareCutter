package com.sharecutter.backend.dto.analytics;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TargetWeightUpdateRequest(

        @NotNull(
                message =
                        "Target weight percent must not be null"
        )
        @DecimalMin(
                value = "0",
                inclusive = true,
                message =
                        "Target weight percent must not be negative"
        )
        @DecimalMax(
                value = "100",
                inclusive = true,
                message =
                        "Target weight percent must not exceed 100"
        )
        BigDecimal targetWeightPercent

) {
}