CREATE TABLE portfolios
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    name VARCHAR(120) NOT NULL,

    creation_method VARCHAR(30) NOT NULL,

    initial_value NUMERIC(19, 4) NOT NULL,

    current_value NUMERIC(19, 4) NOT NULL,

    total_realized_profit NUMERIC(19, 4)
        NOT NULL
        DEFAULT 0,

    total_unrealized_profit NUMERIC(19, 4)
        NOT NULL
        DEFAULT 0,

    total_return_percent NUMERIC(19, 6)
        NOT NULL
        DEFAULT 0,

    created_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP
    WITH TIME ZONE NOT NULL,

    deleted_at TIMESTAMP
    WITH TIME ZONE,

    CONSTRAINT fk_portfolios_user
        FOREIGN KEY
    (user_id)
        REFERENCES users
    (id),

    CONSTRAINT uq_portfolios_user_name
        UNIQUE
    (user_id, name),

    CONSTRAINT chk_portfolios_name_not_blank
        CHECK
    (LENGTH
    (TRIM
    (name)) > 0),

    CONSTRAINT chk_portfolios_initial_value_positive
        CHECK
    (initial_value > 0),

    CONSTRAINT chk_portfolios_current_value_non_negative
        CHECK
    (current_value >= 0),

    CONSTRAINT chk_portfolios_creation_method
        CHECK
    (
            creation_method IN
    (
                'BY_AMOUNT',
                'BY_HOLDINGS'
            )
        )
);

    CREATE INDEX idx_portfolios_user_id
    ON portfolios (user_id);

    CREATE INDEX idx_portfolios_user_active
    ON portfolios (user_id)
    WHERE deleted_at IS NULL;