package com.sharecutter.backend.dto.marketdata;

import com.sharecutter.backend.domain.enums.AssetType;

public record MarketSymbolSearchResponse(

        String symbol,

        String displayName,

        String exchange,

        String micCode,

        String instrumentType,

        String country,

        String currency,

        AssetType assetType

) {
}