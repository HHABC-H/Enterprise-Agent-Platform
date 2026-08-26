CREATE TABLE platform_user (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    tenant_id TEXT NOT NULL,
    roles TEXT[] NOT NULL DEFAULT '{USER}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX platform_user_tenant_idx ON platform_user (tenant_id);
