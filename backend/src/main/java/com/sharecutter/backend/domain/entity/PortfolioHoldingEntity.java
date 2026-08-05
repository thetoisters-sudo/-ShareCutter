package com.sharecutter.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "portfolio_holdings")
public class PortfolioHoldingEntity extends BaseEntity {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "portfolio_id",
            nullable = false
    )
    private PortfolioEntity portfolio;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "asset_id",
            nullable = false
    )
    private AssetEntity asset;

    @Column(
            name = "quantity",
            nullable = false,
            precision = 24,
            scale = 8
    )
    private BigDecimal quantity = ZERO;

    @Column(
            name = "average_cost",
            nullable = false,
            precision = 24,
            scale = 8
    )
    private BigDecimal averageCost = ZERO;

    @Column(
            name = "current_price",
            nullable = false,
            precision = 24,
            scale = 8
    )
    private BigDecimal currentPrice = ZERO;

    @Column(
            name = "total_cost",
            nullable = false,
            precision = 24,
            scale = 8
    )
    private BigDecimal totalCost = ZERO;

    @Column(
            name = "market_value",
            nullable = false,
            precision = 24,
            scale = 8
    )
    private BigDecimal marketValue = ZERO;

    @Column(
            name = "realized_profit",
            nullable = false,
            precision = 24,
            scale = 8
    )
    private BigDecimal realizedProfit = ZERO;

    @Column(
            name = "unrealized_profit",
            nullable = false,
            precision = 24,
            scale = 8
    )
    private BigDecimal unrealizedProfit = ZERO;

    @Column(
            name = "target_weight_percent",
            nullable = false,
            precision = 9,
            scale = 6
    )
    private BigDecimal targetWeightPercent = ZERO;

    @Column(
            name = "last_calculated_at",
            nullable = false
    )
    private OffsetDateTime lastCalculatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected PortfolioHoldingEntity() {
    }

    public PortfolioHoldingEntity(
            PortfolioEntity portfolio,
            AssetEntity asset
    ) {
        setPortfolio(portfolio);
        setAsset(asset);

        this.quantity = ZERO;
        this.averageCost = ZERO;
        this.currentPrice = ZERO;
        this.totalCost = ZERO;
        this.marketValue = ZERO;
        this.realizedProfit = ZERO;
        this.unrealizedProfit = ZERO;
        this.targetWeightPercent = ZERO;
        this.lastCalculatedAt =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );
    }

    public PortfolioEntity getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(
            PortfolioEntity portfolio
    ) {
        if (portfolio == null) {
            throw new IllegalArgumentException(
                    "Holding portfolio must not be null"
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
        if (asset == null) {
            throw new IllegalArgumentException(
                    "Holding asset must not be null"
            );
        }

        this.asset = asset;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void updateQuantity(
            BigDecimal quantity
    ) {
        this.quantity = requireNonNegative(
                quantity,
                "Holding quantity"
        );

        recalculateDerivedValues();
    }

    public BigDecimal getAverageCost() {
        return averageCost;
    }

    public void updateAverageCost(
            BigDecimal averageCost
    ) {
        this.averageCost = requireNonNegative(
                averageCost,
                "Holding average cost"
        );

        recalculateDerivedValues();
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void updateCurrentPrice(
            BigDecimal currentPrice
    ) {
        this.currentPrice = requireNonNegative(
                currentPrice,
                "Holding current price"
        );

        recalculateDerivedValues();
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public BigDecimal getRealizedProfit() {
        return realizedProfit;
    }

    public void updateRealizedProfit(
            BigDecimal realizedProfit
    ) {
        this.realizedProfit = requireValue(
                realizedProfit,
                "Holding realized profit"
        );

        touchCalculationTime();
    }

    public BigDecimal getUnrealizedProfit() {
        return unrealizedProfit;
    }

    public BigDecimal getTargetWeightPercent() {
        return targetWeightPercent;
    }

    public void updateTargetWeightPercent(
            BigDecimal targetWeightPercent
    ) {
        BigDecimal normalizedTargetWeight =
                requireNonNegative(
                        targetWeightPercent,
                        "Holding target weight percent"
                );

        if (
                normalizedTargetWeight.compareTo(
                        ONE_HUNDRED
                ) > 0
        ) {
            throw new IllegalArgumentException(
                    "Holding target weight percent "
                            + "must not exceed 100"
            );
        }

        this.targetWeightPercent =
                normalizedTargetWeight;

        touchCalculationTime();
    }

    public OffsetDateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isEmpty() {
        return quantity.compareTo(ZERO) == 0;
    }

    public void replaceCalculatedState(
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal currentPrice,
            BigDecimal realizedProfit
    ) {
        this.quantity = requireNonNegative(
                quantity,
                "Holding quantity"
        );

        this.averageCost = requireNonNegative(
                averageCost,
                "Holding average cost"
        );

        this.currentPrice = requireNonNegative(
                currentPrice,
                "Holding current price"
        );

        this.realizedProfit = requireValue(
                realizedProfit,
                "Holding realized profit"
        );

        recalculateDerivedValues();
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
        touchCalculationTime();
    }

    private void recalculateDerivedValues() {
        totalCost = quantity.multiply(
                averageCost
        );

        marketValue = quantity.multiply(
                currentPrice
        );

        unrealizedProfit = marketValue.subtract(
                totalCost
        );

        touchCalculationTime();
    }

    private void touchCalculationTime() {
        lastCalculatedAt =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );
    }

    private static BigDecimal requireNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        requireValue(
                value,
                fieldName
        );

        if (value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must not be negative"
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
                    fieldName
                            + " must not be null"
            );
        }

        return value;
    }
}