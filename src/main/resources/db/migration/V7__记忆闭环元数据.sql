ALTER TABLE user_long_term_memory
    ADD COLUMN IF NOT EXISTS source TEXT NOT NULL DEFAULT '历史导入',
    ADD COLUMN IF NOT EXISTS confidence DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS obsolete BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS replaced_by TEXT,
    ADD COLUMN IF NOT EXISTS dedupe_key TEXT,
    ADD COLUMN IF NOT EXISTS conflict_key TEXT;

UPDATE user_long_term_memory
SET dedupe_key = memory_id
WHERE dedupe_key IS NULL;

ALTER TABLE user_long_term_memory
    ALTER COLUMN dedupe_key SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS user_long_term_memory_owner_dedupe_idx
    ON user_long_term_memory (tenant_id, user_id, dedupe_key);
CREATE INDEX IF NOT EXISTS user_long_term_memory_owner_active_idx
    ON user_long_term_memory (tenant_id, user_id, obsolete, created_at DESC);
CREATE INDEX IF NOT EXISTS user_long_term_memory_conflict_idx
    ON user_long_term_memory (tenant_id, user_id, conflict_key)
    WHERE obsolete = FALSE AND conflict_key <> '';
