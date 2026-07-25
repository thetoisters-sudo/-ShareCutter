CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_email_not_blank
        CHECK (length(trim(email)) > 0),

    CONSTRAINT chk_users_password_hash_not_blank
        CHECK (length(trim(password_hash)) > 0),

    CONSTRAINT chk_users_first_name_not_blank
        CHECK (length(trim(first_name)) > 0),

    CONSTRAINT chk_users_last_name_not_blank
        CHECK (length(trim(last_name)) > 0),

    CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'ADMIN')),

    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED'))
);

CREATE UNIQUE INDEX ux_users_email_lower
    ON users (lower(email));

CREATE INDEX ix_users_status
    ON users (status);

CREATE INDEX ix_users_role
    ON users (role);

CREATE INDEX ix_users_created_at
    ON users (created_at);
