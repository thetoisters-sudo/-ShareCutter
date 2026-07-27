package com.sharecutter.backend.exception;

import java.util.UUID;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(UUID assetId) {
        super("Asset not found with id: " + assetId);
    }
}