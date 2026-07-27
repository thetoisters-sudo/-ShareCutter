package com.sharecutter.backend.dto.transaction;

import com.sharecutter.backend.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(

        UUID id,

        UUID portfolioId,

        UUID assetId,

        TransactionType transactionType,

        BigDecimal quantity,

        BigDecimal unitPrice,

        BigDecimal fee,

        BigDecimal totalAmount,

        String currency,

        OffsetDateTime executedAt,

        String notes,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt

) {
}