package com.sharecutter.backend.dto.asset;

import com.sharecutter.backend.domain.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssetCreateRequest(

        @NotBlank(message = "Asset symbol must not be blank")
        @Size(
                max = 30,
                message = "Asset symbol must not exceed 30 characters"
        )
        String symbol,

        @NotBlank(message = "Asset display name must not be blank")
        @Size(
                max = 160,
                message = "Asset display name must not exceed 160 characters"
        )
        String displayName,

        @NotNull(message = "Asset type is required")
        AssetType assetType,

        @NotBlank(message = "Asset currency must not be blank")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Asset currency must contain exactly 3 letters"
        )
        String currency,

        @Size(
                max = 12,
                message = "Asset ISIN must not exceed 12 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9]{12}$",
                message = "Asset ISIN must contain exactly 12 letters or digits"
        )
        String isin,

        @Size(
                max = 40,
                message = "Asset exchange must not exceed 40 characters"
        )
        String exchange,

        @Size(
                max = 2000,
                message = "Asset notes must not exceed 2000 characters"
        )
        String notes

) {
}