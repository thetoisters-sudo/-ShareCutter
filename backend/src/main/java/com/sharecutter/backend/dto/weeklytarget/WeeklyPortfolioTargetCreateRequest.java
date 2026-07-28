package com.sharecutter.backend.dto.weeklytarget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record WeeklyPortfolioTargetCreateRequest(

        @NotNull(message = "Week start date is required")
        LocalDate weekStartDate,

        @NotEmpty(message = "At least one weekly target is required")
        @Size(
                max = 10,
                message = "A weekly target set must not contain more than 10 assets"
        )
        List<@Valid WeeklyTargetItemRequest> targets

) {

    public WeeklyPortfolioTargetCreateRequest {
        targets = targets == null
                ? null
                : List.copyOf(targets);
    }
}