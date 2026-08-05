CREATE TABLE portfolio_holdings
(
    id UUID PRIMARY KEY,

    portfolio_id UUID NOT NULL,

    asset_id UUID NOT NULL,

    quantity NUMERIC(24, 8)
        NOT NULL
        DEFAULT 0,

    average_cost NUMERIC(24, 8)
        NOT NULL
        DEFAULT 0,

    current_price NUMERIC(24, 8)
        NOT NULL
        DEFAULT 0,

    total_cost NUMERIC(24, 8)
        NOT NULL
        DEFAULT 0,

    market_value NUMERIC(24, 8)
        NOT NULL
        DEFAULT 0,

    realized_profit NUMERIC(24, 8)
        NOT NULL
        DEFAULT 0,

    unrealized_profit NUMERIC(24, 8)
        NOT NULL
        DEFAULT 0,

    last_calculated_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    created_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    deleted_at TIMESTAMP
    WITH TIME ZONE,

    CONSTRAINT fk_portfolio_holdings_portfolio
        FOREIGN KEY
    (portfolio_id)
        REFERENCES portfolios
    (id),

    CONSTRAINT fk_portfolio_holdings_asset
        FOREIGN KEY
    (asset_id)
        REFERENCES assets
    (id),

    CONSTRAINT chk_portfolio_holdings_quantity_non_negative
        CHECK
    (quantity >= 0),

    CONSTRAINT chk_portfolio_holdings_average_cost_non_negative
        CHECK
    (average_cost >= 0),

    CONSTRAINT chk_portfolio_holdings_current_price_non_negative
        CHECK
    (current_price >= 0),

    CONSTRAINT chk_portfolio_holdings_total_cost_non_negative
        CHECK
    (total_cost >= 0),

    CONSTRAINT chk_portfolio_holdings_market_value_non_negative
        CHECK
    (market_value >= 0)
);

    CREATE UNIQUE INDEX uq_portfolio_holdings_portfolio_asset_active
    ON portfolio_holdings
    (
        portfolio_id,
        asset_id
    )
    WHERE deleted_at IS NULL;

    CREATE INDEX idx_portfolio_holdings_portfolio_id
    ON portfolio_holdings
    (
        portfolio_id
    );

    CREATE INDEX idx_portfolio_holdings_asset_id
    ON portfolio_holdings
    (
        asset_id
    );

    CREATE INDEX idx_portfolio_holdings_portfolio_active
    ON portfolio_holdings
    (
        portfolio_id
    )
    WHERE deleted_at IS NULL;

    CREATE INDEX idx_portfolio_holdings_asset_active
    ON portfolio_holdings
    (
        asset_id
    )
    WHERE deleted_at IS NULL;