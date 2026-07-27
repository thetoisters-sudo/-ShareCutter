package com.sharecutter.backend.domain.entity;

import com.sharecutter.backend.domain.enums.AssetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.regex.Pattern;

@Entity
@Table(name = "assets")
public class AssetEntity extends BaseEntity {

    private static final Pattern CURRENCY_PATTERN =
            Pattern.compile("^[A-Z]{3}$");

    private static final Pattern ISIN_PATTERN =
            Pattern.compile("^[A-Z0-9]{12}$");

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
            name = "symbol",
            nullable = false,
            length = 30
    )
    private String symbol;

    @Column(
            name = "display_name",
            nullable = false,
            length = 160
    )
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "asset_type",
            nullable = false,
            length = 30
    )
    private AssetType assetType;

    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(
            name = "isin",
            length = 12
    )
    private String isin;

    @Column(
            name = "exchange",
            length = 40
    )
    private String exchange;

    @Column(
            name = "notes",
            length = 2000
    )
    private String notes;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected AssetEntity() {
    }

    public AssetEntity(
            PortfolioEntity portfolio,
            String symbol,
            String displayName,
            AssetType assetType,
            String currency,
            String isin,
            String exchange,
            String notes
    ) {
        setPortfolio(portfolio);
        setSymbol(symbol);
        setDisplayName(displayName);
        setAssetType(assetType);
        setCurrency(currency);
        setIsin(isin);
        setExchange(exchange);
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
                    "Asset portfolio must not be null"
            );
        }

        this.portfolio = portfolio;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        String normalizedSymbol = requireText(
                symbol,
                "Asset symbol"
        ).toUpperCase(Locale.ROOT);

        if (normalizedSymbol.length() > 30) {
            throw new IllegalArgumentException(
                    "Asset symbol must not exceed 30 characters"
            );
        }

        this.symbol = normalizedSymbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(
            String displayName
    ) {
        String normalizedDisplayName = requireText(
                displayName,
                "Asset display name"
        );

        if (normalizedDisplayName.length() > 160) {
            throw new IllegalArgumentException(
                    "Asset display name must not exceed 160 characters"
            );
        }

        this.displayName = normalizedDisplayName;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(
            AssetType assetType
    ) {
        if (assetType == null) {
            throw new IllegalArgumentException(
                    "Asset type must not be null"
            );
        }

        this.assetType = assetType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        String normalizedCurrency = requireText(
                currency,
                "Asset currency"
        ).toUpperCase(Locale.ROOT);

        if (!CURRENCY_PATTERN
                .matcher(normalizedCurrency)
                .matches()) {
            throw new IllegalArgumentException(
                    "Asset currency must contain exactly "
                            + "three uppercase letters"
            );
        }

        this.currency = normalizedCurrency;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        if (isin == null) {
            this.isin = null;
            return;
        }

        String normalizedIsin = requireText(
                isin,
                "Asset ISIN"
        ).toUpperCase(Locale.ROOT);

        if (!ISIN_PATTERN
                .matcher(normalizedIsin)
                .matches()) {
            throw new IllegalArgumentException(
                    "Asset ISIN must contain exactly "
                            + "12 uppercase letters or digits"
            );
        }

        this.isin = normalizedIsin;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        if (exchange == null) {
            this.exchange = null;
            return;
        }

        String normalizedExchange = requireText(
                exchange,
                "Asset exchange"
        );

        if (normalizedExchange.length() > 40) {
            throw new IllegalArgumentException(
                    "Asset exchange must not exceed 40 characters"
            );
        }

        this.exchange = normalizedExchange;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        if (notes == null) {
            this.notes = null;
            return;
        }

        String normalizedNotes = requireText(
                notes,
                "Asset notes"
        );

        if (normalizedNotes.length() > 2000) {
            throw new IllegalArgumentException(
                    "Asset notes must not exceed 2000 characters"
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