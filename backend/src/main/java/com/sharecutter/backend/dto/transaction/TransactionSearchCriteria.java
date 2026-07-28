package com.sharecutter.backend.dto.transaction;

import com.sharecutter.backend.domain.enums.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionSearchCriteria(

        UUID assetId,

        TransactionType transactionType,

        OffsetDateTime startDate,

        OffsetDateTime endDate,

        @Min(
                value = 0,
                message = "Page index must be zero or greater"
        )
        Integer page,

        @Min(
                value = 1,
                message = "Page size must be at least 1"
        )
        @Max(
                value = 200,
                message = "Page size must not exceed 200"
        )
        Integer size

) {
}