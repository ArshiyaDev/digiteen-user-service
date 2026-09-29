CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(32),
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT users_contact_required CHECK (email IS NOT NULL OR phone IS NOT NULL),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_phone_unique UNIQUE (phone),
    CONSTRAINT users_status_valid CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens(user_id);
CREATE INDEX refresh_tokens_expires_at_idx ON refresh_tokens(expires_at);
