CREATE TABLE IF NOT EXISTS user_long_term_memory (
    memory_id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    session_id TEXT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1024) NOT NULL,
    memory_type TEXT NOT NULL CHECK (memory_type IN ('SEMANTIC', 'EPISODIC', 'PROCEDURAL')),
    importance DOUBLE PRECISION NOT NULL CHECK (importance >= 0 AND importance <= 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS user_long_term_memory_owner_idx ON user_long_term_memory (tenant_id, user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS user_long_term_memory_embedding_idx ON user_long_term_memory USING hnsw (embedding vector_cosine_ops);
