ALTER TABLE portfolios
DROP CONSTRAINT IF EXISTS uq_portfolios_user_name;

DROP INDEX IF EXISTS uq_portfolios_user_name_active;

CREATE UNIQUE INDEX uq_portfolios_user_name_active
ON portfolios (
    user_id,
    LOWER(name)
)
WHERE deleted_at IS NULL;