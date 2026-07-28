package com.sharecutter.backend.exception;

public class InvalidWeeklyPortfolioTargetException
        extends RuntimeException {

    public InvalidWeeklyPortfolioTargetException(
            String message
    ) {
        super(message);
    }
}