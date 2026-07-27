package com.sharecutter.backend.dto.portfolio;

import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PortfolioCreateRequest(

        @NotBlank(message = "Portfolio name must not be blank")
        @Size(
                max = 100,
                message = "Portfolio name must not exceed 100 characters"
        )
        String name,

        @NotNull(message = "Portfolio creation method is required")
        PortfolioCreationMethod creationMethod,

        @NotNull(message = "Initial value is required")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "Initial value must be greater than zero"
        )
        @Digits(
                integer = 15,
                fraction = 4,
                message = "Initial value must contain up to 15 integer digits and 4 decimal places"
        )
        BigDecimal initialValue

) {
}