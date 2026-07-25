package com.sharecutter.backend.dto.auth;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    public TokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {
        this(
                accessToken,
                refreshToken,
                BEARER_TOKEN_TYPE,
                expiresIn
        );
    }
}