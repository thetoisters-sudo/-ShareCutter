package com.sharecutter.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "weekly_portfolio_targets")
public class WeeklyPortfolioTargetEntity extends BaseEntity {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
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
            name = "week_start_date",
            nullable = false
    )
    private LocalDate weekStartDate;

    @Column(
            name = "target_percentage",
            nullable = false,
            precision = 7,
            scale = 4
    )
    private BigDecimal targetPercentage;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected WeeklyPortfolioTargetEntity() {
    }

    public WeeklyPortfolioTargetEntity(
            PortfolioEntity portfolio,
            AssetEntity asset,
            LocalDate weekStartDate,
            BigDecimal targetPercentage
    ) {
        setPortfolio(portfolio);
        setAsset(asset);
        setWeekStartDate(weekStartDate);
        setTargetPercentage(targetPercentage);
    }

    public PortfolioEntity getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(
            PortfolioEntity portfolio
    ) {
        if (portfolio == null) {
            throw new IllegalArgumentException(
                    "Weekly target portfolio must not be null"
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
                    "Weekly target asset must not be null"
            );
        }

        this.asset = asset;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(
            LocalDate weekStartDate
    ) {
        if (weekStartDate == null) {
            throw new IllegalArgumentException(
                    "Week start date must not be null"
            );
        }

        if (weekStartDate.getDayOfWeek()
                != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException(
                    "Week start date must be a Monday"
            );
        }

        this.weekStartDate = weekStartDate;
    }

    public BigDecimal getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(
            BigDecimal targetPercentage
    ) {
        if (targetPercentage == null) {
            throw new IllegalArgumentException(
                    "Target percentage must not be null"
            );
        }

        if (targetPercentage.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Target percentage must not be negative"
            );
        }

        if (targetPercentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(
                    "Target percentage must not exceed 100"
            );
        }

        this.targetPercentage = targetPercentage;
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
}