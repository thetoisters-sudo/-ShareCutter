package com.sharecutter.backend.exception;

import java.util.UUID;

public class AssetAlreadyExistsException extends RuntimeException {

    public AssetAlreadyExistsException(
            UUID portfolioId,
            String assetSymbol
    ) {
        super(
                "Asset already exists for portfolio "
                        + portfolioId
                        + " with symbol: "
                        + assetSymbol
        );
    }
}