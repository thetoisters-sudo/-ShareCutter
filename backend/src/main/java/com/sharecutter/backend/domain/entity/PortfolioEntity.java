package com.sharecutter.backend.domain.entity;

import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
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

@Entity
@Table(name = "portfolios")
public class PortfolioEntity extends BaseEntity {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private UserEntity user;

    @Column(
            name = "name",
            nullable = false,
            length = 120
    )
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "creation_method",
            nullable = false,
            length = 30
    )
    private PortfolioCreationMethod creationMethod;

    @Column(
            name = "initial_value",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal initialValue;

    @Column(
            name = "current_value",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal currentValue;

    @Column(
            name = "total_realized_profit",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal totalRealizedProfit = ZERO;

    @Column(
            name = "total_unrealized_profit",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal totalUnrealizedProfit = ZERO;

    @Column(
            name = "total_return_percent",
            nullable = false,
            precision = 19,
            scale = 6
    )
    private BigDecimal totalReturnPercent = ZERO;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected PortfolioEntity() {
    }

    public PortfolioEntity(
            UserEntity user,
            String name,
            PortfolioCreationMethod creationMethod,
            BigDecimal initialValue
    ) {
        setUser(user);
        setName(name);
        setCreationMethod(creationMethod);
        setInitialValue(initialValue);

        this.currentValue = initialValue;
        this.totalRealizedProfit = ZERO;
        this.totalUnrealizedProfit = ZERO;
        this.totalReturnPercent = ZERO;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "Portfolio user must not be null"
            );
        }

        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(
                name,
                "Portfolio name"
        );
    }

    public PortfolioCreationMethod getCreationMethod() {
        return creationMethod;
    }

    public void setCreationMethod(
            PortfolioCreationMethod creationMethod
    ) {
        if (creationMethod == null) {
            throw new IllegalArgumentException(
                    "Portfolio creation method must not be null"
            );
        }

        this.creationMethod = creationMethod;
    }

    public BigDecimal getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(BigDecimal initialValue) {
        this.initialValue = requirePositive(
                initialValue,
                "Initial value"
        );
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void updateCurrentValue(BigDecimal currentValue) {
        this.currentValue = requireNonNegative(
                currentValue,
                "Current value"
        );

        recalculateTotalReturnPercent();
    }

    public BigDecimal getTotalRealizedProfit() {
        return totalRealizedProfit;
    }

    public void updateTotalRealizedProfit(
            BigDecimal totalRealizedProfit
    ) {
        this.totalRealizedProfit = requireValue(
                totalRealizedProfit,
                "Total realized profit"
        );
    }

    public BigDecimal getTotalUnrealizedProfit() {
        return totalUnrealizedProfit;
    }

    public void updateTotalUnrealizedProfit(
            BigDecimal totalUnrealizedProfit
    ) {
        this.totalUnrealizedProfit = requireValue(
                totalUnrealizedProfit,
                "Total unrealized profit"
        );
    }

    public BigDecimal getTotalReturnPercent() {
        return totalReturnPercent;
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

    private void recalculateTotalReturnPercent() {
        if (initialValue == null
                || initialValue.compareTo(ZERO) <= 0) {
            totalReturnPercent = ZERO;
            return;
        }

        totalReturnPercent = currentValue
                .subtract(initialValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        initialValue,
                        6,
                        java.math.RoundingMode.HALF_UP
                );
    }

    private static BigDecimal requirePositive(
            BigDecimal value,
            String fieldName
    ) {
        requireValue(value, fieldName);

        if (value.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero"
            );
        }

        return value;
    }

    private static BigDecimal requireNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        requireValue(value, fieldName);

        if (value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }

        return value;
    }

    private static BigDecimal requireValue(
            BigDecimal value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null"
            );
        }

        return value;
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