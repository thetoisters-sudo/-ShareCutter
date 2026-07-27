package com.sharecutter.backend.dto.portfolio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PortfolioValueUpdateRequest(

        @NotNull(message = "Current value is required")
        @DecimalMin(
                value = "0.0",
                inclusive = true,
                message = "Current value must not be negative"
        )
        @Digits(
                integer = 15,
                fraction = 4,
                message = "Current value must contain up to 15 integer digits and 4 decimal places"
        )
        BigDecimal currentValue

) {
}