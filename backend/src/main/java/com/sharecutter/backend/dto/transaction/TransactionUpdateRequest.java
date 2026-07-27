package com.sharecutter.backend.dto.transaction;

import com.sharecutter.backend.domain.enums.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionUpdateRequest(

        UUID assetId,

        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        @Positive(
                message = "Transaction quantity must be greater than zero"
        )
        @Digits(
                integer = 16,
                fraction = 8,
                message = "Transaction quantity must contain "
                        + "up to 16 integer digits and 8 decimal digits"
        )
        BigDecimal quantity,

        @PositiveOrZero(
                message = "Transaction unit price must not be negative"
        )
        @Digits(
                integer = 16,
                fraction = 8,
                message = "Transaction unit price must contain "
                        + "up to 16 integer digits and 8 decimal digits"
        )
        BigDecimal unitPrice,

        @PositiveOrZero(
                message = "Transaction fee must not be negative"
        )
        @Digits(
                integer = 16,
                fraction = 8,
                message = "Transaction fee must contain "
                        + "up to 16 integer digits and 8 decimal digits"
        )
        BigDecimal fee,

        @NotNull(message = "Transaction total amount is required")
        @PositiveOrZero(
                message = "Transaction total amount must not be negative"
        )
        @Digits(
                integer = 16,
                fraction = 8,
                message = "Transaction total amount must contain "
                        + "up to 16 integer digits and 8 decimal digits"
        )
        BigDecimal totalAmount,

        @NotBlank(message = "Transaction currency must not be blank")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Transaction currency must contain exactly 3 letters"
        )
        String currency,

        @NotNull(message = "Transaction execution time is required")
        OffsetDateTime executedAt,

        @Size(
                max = 2000,
                message = "Transaction notes must not exceed 2000 characters"
        )
        String notes

) {
}