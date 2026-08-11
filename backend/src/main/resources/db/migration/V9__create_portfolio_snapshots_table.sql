CREATE TABLE portfolio_snapshots
(
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    current_value NUMERIC(19, 4) NOT NULL,
    cash_balance NUMERIC(19, 4) NOT NULL,
    holdings_market_value NUMERIC(19, 4) NOT NULL,
    total_profit NUMERIC(19, 4) NOT NULL,
    total_return_percent NUMERIC(19, 6) NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_portfolio_snapshots_portfolio
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolios(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_portfolio_snapshots_portfolio_captured
    ON portfolio_snapshots (
        portfolio_id,
        captured_at
    );