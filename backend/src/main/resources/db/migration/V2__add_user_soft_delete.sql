ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX ix_users_deleted_at
    ON users (deleted_at);

CREATE INDEX ix_users_active_email_lower
    ON users (lower(email))
    WHERE deleted_at IS NULL;