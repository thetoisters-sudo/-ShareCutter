package com.sharecutter.backend.repository.projection;

import com.sharecutter.backend.domain.enums.AssetType;

import java.math.BigDecimal;
import java.util.UUID;

public interface AssetAllocationProjection {

    UUID getAssetId();

    String getSymbol();

    String getDisplayName();

    AssetType getAssetType();

    String getCurrency();

    BigDecimal getBoughtQuantity();

    BigDecimal getSoldQuantity();

    BigDecimal getTotalBuyAmount();

    BigDecimal getTotalSellAmount();
}