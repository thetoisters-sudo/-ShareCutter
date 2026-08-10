package com.sharecutter.backend.dto.portfolio;

import com.sharecutter.backend.domain.enums.AssetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioCreateFromHoldingsRequest(

        @NotBlank(
                message = "Portfolio name must not be blank"
        )
        @Size(
                max = 100,
                message =
                        "Portfolio name must not exceed 100 characters"
        )
        String name,

        @NotNull(
                message =
                        "Holdings list must not be null"
        )
        List<
                @Valid HoldingRequest
                > holdings,

        @NotNull(
                message =
                        "Initial cash must not be null"
        )
        @DecimalMin(
                value = "0.00000000",
                inclusive = true,
                message =
                        "Initial cash must not be negative"
        )
        @Digits(
                integer = 16,
                fraction = 8,
                message =
                        "Initial cash must contain up to "
                                + "16 integer digits and "
                                + "8 decimal places"
        )
        BigDecimal initialCash

) {

    public record HoldingRequest(

            @NotBlank(
                    message =
                            "Asset symbol must not be blank"
            )
            @Size(
                    max = 30,
                    message =
                            "Asset symbol must not exceed 30 characters"
            )
            String symbol,

            @NotBlank(
                    message =
                            "Asset display name must not be blank"
            )
            @Size(
                    max = 160,
                    message =
                            "Asset display name must not exceed 160 characters"
            )
            String displayName,

            @NotNull(
                    message =
                            "Asset type is required"
            )
            AssetType assetType,

            @NotBlank(
                    message =
                            "Asset currency must not be blank"
            )
            @Pattern(
                    regexp = "^[A-Za-z]{3}$",
                    message =
                            "Asset currency must contain exactly 3 letters"
            )
            String currency,

            @Size(
                    max = 40,
                    message =
                            "Asset exchange must not exceed 40 characters"
            )
            String exchange,

            @NotNull(
                    message =
                            "Holding quantity must not be null"
            )
            @DecimalMin(
                    value = "0.00000001",
                    inclusive = true,
                    message =
                            "Holding quantity must be greater than zero"
            )
            @Digits(
                    integer = 16,
                    fraction = 8,
                    message =
                            "Holding quantity must contain up to "
                                    + "16 integer digits and "
                                    + "8 decimal places"
            )
            BigDecimal quantity

    ) {
    }
}