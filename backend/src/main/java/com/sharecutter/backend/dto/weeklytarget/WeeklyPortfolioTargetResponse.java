package com.sharecutter.backend.dto.weeklytarget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record WeeklyPortfolioTargetResponse(

        UUID portfolioId,

        String portfolioName,

        LocalDate weekStartDate,

        BigDecimal totalTargetPercentage,

        long targetCount,

        List<WeeklyTargetItemResponse> targets,

        OffsetDateTime generatedAt

) {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    public WeeklyPortfolioTargetResponse {
        if (portfolioId == null) {
            throw new IllegalArgumentException(
                    "Portfolio id must not be null"
            );
        }

        portfolioName = requireText(
                portfolioName,
                "Portfolio name"
        );

        if (weekStartDate == null) {
            throw new IllegalArgumentException(
                    "Week start date must not be null"
            );
        }

        if (totalTargetPercentage == null) {
            throw new IllegalArgumentException(
                    "Total target percentage must not be null"
            );
        }

        if (totalTargetPercentage.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Total target percentage must not be negative"
            );
        }

        if (totalTargetPercentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(
                    "Total target percentage must not exceed 100"
            );
        }

        if (targetCount < 0) {
            throw new IllegalArgumentException(
                    "Target count must not be negative"
            );
        }

        targets = targets == null
                ? List.of()
                : List.copyOf(targets);

        if (targetCount != targets.size()) {
            throw new IllegalArgumentException(
                    "Target count must match target list size"
            );
        }

        if (generatedAt == null) {
            throw new IllegalArgumentException(
                    "Generation time must not be null"
            );
        }
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }
}