package com.sharecutter.backend.exception;

import java.util.UUID;

public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(UUID portfolioId) {
        super("Portfolio not found with id: " + portfolioId);
    }
}