package com.sharecutter.backend.dto.asset;

import com.sharecutter.backend.domain.enums.AssetType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssetResponse(

        UUID id,

        UUID portfolioId,

        String symbol,

        String displayName,

        AssetType assetType,

        String currency,

        String isin,

        String exchange,

        String notes,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt

) {
}