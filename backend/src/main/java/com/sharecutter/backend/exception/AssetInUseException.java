package com.sharecutter.backend.exception;

import java.util.UUID;

public class AssetInUseException extends RuntimeException {

    public AssetInUseException(
            UUID assetId,
            String reason
    ) {
        super(
                "Asset "
                        + assetId
                        + " cannot be deleted: "
                        + requireReason(reason)
        );
    }

    private static String requireReason(
            String reason
    ) {
        if (
                reason == null
                        || reason.isBlank()
        ) {
            return "the asset is still in use";
        }

        return reason.trim();
    }
}