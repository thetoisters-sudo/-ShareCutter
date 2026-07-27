package com.sharecutter.backend.exception;

import java.util.UUID;

public class PortfolioAlreadyExistsException extends RuntimeException {

    public PortfolioAlreadyExistsException(
            UUID userId,
            String portfolioName
    ) {
        super(
                "Portfolio already exists for user "
                        + userId
                        + " with name: "
                        + portfolioName
        );
    }
}