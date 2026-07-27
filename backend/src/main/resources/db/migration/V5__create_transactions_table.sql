CREATE TABLE transactions
(
    id UUID PRIMARY KEY,

    portfolio_id UUID NOT NULL,

    asset_id UUID,

    transaction_type VARCHAR(30) NOT NULL,

    quantity NUMERIC(24, 8),

    unit_price NUMERIC(24, 8),

    fee NUMERIC(24, 8) NOT NULL DEFAULT 0,

    total_amount NUMERIC(24, 8) NOT NULL,

    currency VARCHAR(3) NOT NULL,

    executed_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    notes VARCHAR
    (2000),

    created_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    deleted_at TIMESTAMP
    WITH TIME ZONE,

    CONSTRAINT fk_transactions_portfolio
        FOREIGN KEY
    (portfolio_id)
        REFERENCES portfolios
    (id),

    CONSTRAINT fk_transactions_asset
        FOREIGN KEY
    (asset_id)
        REFERENCES assets
    (id),

    CONSTRAINT chk_transactions_transaction_type
        CHECK
    (
            transaction_type IN
    (
                'BUY',
                'SELL',
                'DIVIDEND',
                'FEE',
                'DEPOSIT',
                'WITHDRAWAL'
            )
        ),

    CONSTRAINT chk_transactions_quantity_positive
        CHECK
    (
            quantity IS NULL
            OR quantity > 0
        ),

    CONSTRAINT chk_transactions_unit_price_not_negative
        CHECK
    (
            unit_price IS NULL
            OR unit_price >= 0
        ),

    CONSTRAINT chk_transactions_fee_not_negative
        CHECK
    (
            fee >= 0
        ),

    CONSTRAINT chk_transactions_total_amount_not_negative
        CHECK
    (
            total_amount >= 0
        ),

    CONSTRAINT chk_transactions_currency_format
        CHECK
    (
            currency ~ '^[A-Z]{3}$'
        ),

    CONSTRAINT chk_transactions_notes_not_blank
        CHECK
    (
            notes IS NULL
            OR LENGTH
    (TRIM
    (notes)) > 0
        ),

    CONSTRAINT chk_transactions_asset_fields
        CHECK
    (
            transaction_type NOT IN
    (
                'BUY',
                'SELL'
            )
            OR
    (
                asset_id IS NOT NULL
                AND quantity IS NOT NULL
                AND unit_price IS NOT NULL
            )
        )
);

    CREATE INDEX idx_transactions_portfolio_id
    ON transactions
    (
        portfolio_id
    );

    CREATE INDEX idx_transactions_portfolio_active
    ON transactions
    (
        portfolio_id
    )
    WHERE deleted_at IS NULL;

    CREATE INDEX idx_transactions_asset_id
    ON transactions
    (
        asset_id
    )
    WHERE asset_id IS NOT NULL;

    CREATE INDEX idx_transactions_transaction_type
    ON transactions
    (
        transaction_type
    );

    CREATE INDEX idx_transactions_executed_at
    ON transactions
    (
        executed_at
    );

    CREATE INDEX idx_transactions_portfolio_executed_at
    ON transactions
    (
        portfolio_id,
        executed_at DESC
    );

    CREATE INDEX idx_transactions_portfolio_asset_active
    ON transactions
    (
        portfolio_id,
        asset_id
    )
    WHERE deleted_at IS NULL
      AND asset_id IS NOT NULL;