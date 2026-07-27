package com.sharecutter.backend.domain.entity;

import com.sharecutter.backend.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.regex.Pattern;

@Entity
@Table(name = "transactions")
public class TransactionEntity extends BaseEntity {

    private static final Pattern CURRENCY_PATTERN =
            Pattern.compile("^[A-Z]{3}$");

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "portfolio_id",
            nullable = false
    )
    private PortfolioEntity portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private AssetEntity asset;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "transaction_type",
            nullable = false,
            length = 30
    )
    private TransactionType transactionType;

    @Column(
            name = "quantity",
            precision = 24,
            scale = 8
    )
    private BigDecimal quantity;

    @Column(
            name = "unit_price",
            precision = 24,
            scale = 8
    )
    private BigDecimal unitPrice;

    @Column(
            name = "fee",
            nullable = false,
            precision = 24,
            scale = 8
    )
    private BigDecimal fee;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 24,
            scale = 8
    )
    private BigDecimal totalAmount;

    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(
            name = "executed_at",
            nullable = false
    )
    private OffsetDateTime executedAt;

    @Column(
            name = "notes",
            length = 2000
    )
    private String notes;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected TransactionEntity() {
    }

    public TransactionEntity(
            PortfolioEntity portfolio,
            AssetEntity asset,
            TransactionType transactionType,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal fee,
            BigDecimal totalAmount,
            String currency,
            OffsetDateTime executedAt,
            String notes
    ) {
        setPortfolio(portfolio);
        setAsset(asset);
        setTransactionType(transactionType);
        setQuantity(quantity);
        setUnitPrice(unitPrice);
        setFee(fee);
        setTotalAmount(totalAmount);
        setCurrency(currency);
        setExecutedAt(executedAt);
        setNotes(notes);
    }

    public PortfolioEntity getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(
            PortfolioEntity portfolio
    ) {
        if (portfolio == null) {
            throw new IllegalArgumentException(
                    "Transaction portfolio must not be null"
            );
        }

        this.portfolio = portfolio;
    }

    public AssetEntity getAsset() {
        return asset;
    }

    public void setAsset(
            AssetEntity asset
    ) {
        this.asset = asset;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(
            TransactionType transactionType
    ) {
        if (transactionType == null) {
            throw new IllegalArgumentException(
                    "Transaction type must not be null"
            );
        }

        this.transactionType = transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(
            BigDecimal quantity
    ) {
        if (quantity == null) {
            this.quantity = null;
            return;
        }

        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Transaction quantity must be greater than zero"
            );
        }

        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(
            BigDecimal unitPrice
    ) {
        if (unitPrice == null) {
            this.unitPrice = null;
            return;
        }

        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Transaction unit price must not be negative"
            );
        }

        this.unitPrice = unitPrice;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(
            BigDecimal fee
    ) {
        if (fee == null) {
            this.fee = BigDecimal.ZERO;
            return;
        }

        if (fee.signum() < 0) {
            throw new IllegalArgumentException(
                    "Transaction fee must not be negative"
            );
        }

        this.fee = fee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(
            BigDecimal totalAmount
    ) {
        if (totalAmount == null) {
            throw new IllegalArgumentException(
                    "Transaction total amount must not be null"
            );
        }

        if (totalAmount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Transaction total amount must not be negative"
            );
        }

        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(
            String currency
    ) {
        String normalizedCurrency = requireText(
                currency,
                "Transaction currency"
        ).toUpperCase(Locale.ROOT);

        if (!CURRENCY_PATTERN
                .matcher(normalizedCurrency)
                .matches()) {
            throw new IllegalArgumentException(
                    "Transaction currency must contain exactly "
                            + "three uppercase letters"
            );
        }

        this.currency = normalizedCurrency;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(
            OffsetDateTime executedAt
    ) {
        if (executedAt == null) {
            throw new IllegalArgumentException(
                    "Transaction execution time must not be null"
            );
        }

        this.executedAt = executedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(
            String notes
    ) {
        if (notes == null) {
            this.notes = null;
            return;
        }

        String normalizedNotes = requireText(
                notes,
                "Transaction notes"
        );

        if (normalizedNotes.length() > 2000) {
            throw new IllegalArgumentException(
                    "Transaction notes must not exceed 2000 characters"
            );
        }

        this.notes = normalizedNotes;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        if (deletedAt == null) {
            deletedAt = OffsetDateTime.now(
                    ZoneOffset.UTC
            );
        }
    }

    public void restore() {
        deletedAt = null;
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