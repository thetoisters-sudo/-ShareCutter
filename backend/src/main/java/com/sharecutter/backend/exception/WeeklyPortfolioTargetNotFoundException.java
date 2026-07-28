package com.sharecutter.backend.exception;

import java.time.LocalDate;
import java.util.UUID;

public class WeeklyPortfolioTargetNotFoundException
        extends RuntimeException {

    public WeeklyPortfolioTargetNotFoundException(
            UUID portfolioId,
            LocalDate weekStartDate
    ) {
        super(
                "Weekly portfolio targets were not found "
                        + "for portfolio id "
                        + portfolioId
                        + " and week starting "
                        + weekStartDate
        );
    }
}