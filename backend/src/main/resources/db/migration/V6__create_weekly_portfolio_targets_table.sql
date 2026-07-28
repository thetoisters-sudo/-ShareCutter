CREATE TABLE weekly_portfolio_targets
(
    id UUID PRIMARY KEY,

    portfolio_id UUID NOT NULL,
    asset_id UUID NOT NULL,

    week_start_date DATE NOT NULL,

    target_percentage NUMERIC(7, 4) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_weekly_portfolio_targets_portfolio
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolios (id),

    CONSTRAINT fk_weekly_portfolio_targets_asset
        FOREIGN KEY (asset_id)
        REFERENCES assets (id),

    CONSTRAINT chk_weekly_portfolio_targets_percentage
        CHECK (
            target_percentage >= 0
        AND target_percentage <= 100
        )
);

CREATE UNIQUE INDEX uq_weekly_targets_portfolio_week_asset_active
    ON weekly_portfolio_targets (
        portfolio_id,
        week_start_date,
        asset_id
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_weekly_targets_portfolio_week
    ON weekly_portfolio_targets (
        portfolio_id,
        week_start_date
    )
    WHERE deleted_at IS NULL;

CREATE INDEX idx_weekly_targets_asset
    ON weekly_portfolio_targets (
        asset_id
    )
    WHERE deleted_at IS NULL;