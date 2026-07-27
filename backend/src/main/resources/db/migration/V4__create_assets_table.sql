CREATE TABLE assets
(
    id UUID PRIMARY KEY,

    portfolio_id UUID NOT NULL,

    symbol VARCHAR(30) NOT NULL,

    display_name VARCHAR(160) NOT NULL,

    asset_type VARCHAR(30) NOT NULL,

    currency VARCHAR(3) NOT NULL,

    isin VARCHAR(12),

    exchange VARCHAR(40),

    notes VARCHAR(2000),

    created_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    deleted_at TIMESTAMP
    WITH TIME ZONE,

    CONSTRAINT fk_assets_portfolio
        FOREIGN KEY
    (portfolio_id)
        REFERENCES portfolios
    (id),

    CONSTRAINT chk_assets_symbol_not_blank
        CHECK
    (LENGTH
    (TRIM
    (symbol)) > 0),

    CONSTRAINT chk_assets_display_name_not_blank
        CHECK
    (LENGTH
    (TRIM
    (display_name)) > 0),

    CONSTRAINT chk_assets_asset_type
        CHECK
    (
            asset_type IN
    (
                'STOCK',
                'ETF',
                'BOND',
                'FUND',
                'CRYPTO',
                'COMMODITY',
                'FOREX',
                'CASH',
                'OTHER'
            )
        ),

    CONSTRAINT chk_assets_currency_format
        CHECK
    (
            currency ~ '^[A-Z]{3}$'
        ),

    CONSTRAINT chk_assets_isin_format
        CHECK
    (
            isin IS NULL
            OR isin ~ '^[A-Z0-9]{12}$'
        ),

    CONSTRAINT chk_assets_exchange_not_blank
        CHECK
    (
            exchange IS NULL
            OR LENGTH
    (TRIM
    (exchange)) > 0
        ),

    CONSTRAINT chk_assets_notes_not_blank
        CHECK
    (
            notes IS NULL
            OR LENGTH
    (TRIM
    (notes)) > 0
        )
);

    CREATE UNIQUE INDEX uq_assets_portfolio_symbol_active
    ON assets
    (
        portfolio_id,
        UPPER
    (symbol)
    )
    WHERE deleted_at IS NULL;

    CREATE INDEX idx_assets_portfolio_id
    ON assets
    (
        portfolio_id
    );

    CREATE INDEX idx_assets_portfolio_active
    ON assets
    (
        portfolio_id
    )
    WHERE deleted_at IS NULL;

    CREATE INDEX idx_assets_symbol
    ON assets
    (
        UPPER
    (symbol)
    );

    CREATE INDEX idx_assets_asset_type
    ON assets
    (
        asset_type
    );

    CREATE INDEX idx_assets_isin
    ON assets
    (
        isin
    )
    WHERE isin IS NOT NULL;