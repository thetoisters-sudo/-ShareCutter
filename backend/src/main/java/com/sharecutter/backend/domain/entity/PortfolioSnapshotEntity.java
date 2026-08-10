package com.sharecutter.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "portfolio_snapshots")
public class PortfolioSnapshotEntity
        extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "portfolio_id",
            nullable = false
    )
    private PortfolioEntity portfolio;

    @Column(
            name = "current_value",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal currentValue;

    @Column(
            name = "cash_balance",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal cashBalance;

    @Column(
            name = "holdings_market_value",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal holdingsMarketValue;

    @Column(
            name = "total_profit",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal totalProfit;

    @Column(
            name = "total_return_percent",
            nullable = false,
            precision = 19,
            scale = 6
    )
    private BigDecimal totalReturnPercent;

    @Column(
            name = "captured_at",
            nullable = false
    )
    private OffsetDateTime capturedAt;

    protected PortfolioSnapshotEntity() {
    }

    public PortfolioSnapshotEntity(
            PortfolioEntity portfolio,
            BigDecimal currentValue,
            BigDecimal cashBalance,
            BigDecimal holdingsMarketValue,
            BigDecimal totalProfit,
            BigDecimal totalReturnPercent,
            OffsetDateTime capturedAt
    ) {
        this.portfolio = portfolio;
        this.currentValue = currentValue;
        this.cashBalance = cashBalance;
        this.holdingsMarketValue =
                holdingsMarketValue;
        this.totalProfit = totalProfit;
        this.totalReturnPercent =
                totalReturnPercent;
        this.capturedAt = capturedAt;
    }

    public PortfolioEntity getPortfolio() {
        return portfolio;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public BigDecimal getHoldingsMarketValue() {
        return holdingsMarketValue;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public BigDecimal getTotalReturnPercent() {
        return totalReturnPercent;
    }

    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }
}