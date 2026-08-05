ALTER TABLE portfolio_holdings
    ADD COLUMN target_weight_percent NUMERIC
(9, 6)
        NOT NULL
        DEFAULT 0;

ALTER TABLE portfolio_holdings
    ADD CONSTRAINT chk_portfolio_holdings_target_weight_percent
        CHECK (
            target_weight_percent >= 0
            AND target_weight_percent <= 100
        );