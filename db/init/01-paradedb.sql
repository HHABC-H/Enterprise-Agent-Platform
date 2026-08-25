CREATE EXTENSION IF NOT EXISTS pg_search CASCADE;

CREATE TABLE IF NOT EXISTS knowledge_document_revision (
    tenant_id TEXT NOT NULL,
    document_id TEXT NOT NULL,
    markdown TEXT NOT NULL,
    source TEXT NOT NULL,
    version TEXT NOT NULL,
    permission_tags TEXT[] NOT NULL DEFAULT '{}',
    allowed_user_ids TEXT[] NOT NULL DEFAULT '{}',
    content_hash CHAR(64) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, document_id)
);

CREATE TABLE IF NOT EXISTS knowledge_ingestion_task (
    idempotency_key TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    document_id TEXT NOT NULL,
    version TEXT NOT NULL,
    state TEXT NOT NULL CHECK (state IN ('QUEUED', 'PROCESSING', 'SUCCESS', 'FAILED', 'SKIPPED')),
    attempts INTEGER NOT NULL CHECK (attempts >= 0),
    failure_reason TEXT,
    updated_at TIMESTAMPTZ NOT NULL,
    trace_id TEXT
);
CREATE INDEX IF NOT EXISTS knowledge_ingestion_task_document_idx ON knowledge_ingestion_task (tenant_id, document_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    chunk_id TEXT PRIMARY KEY,
    parent_chunk_id TEXT,
    tenant_id TEXT NOT NULL,
    document_id TEXT NOT NULL,
    version TEXT NOT NULL,
    content TEXT NOT NULL,
    permission_tags TEXT[] NOT NULL DEFAULT '{}',
    allowed_user_ids TEXT[] NOT NULL DEFAULT '{}'
);
CREATE INDEX IF NOT EXISTS knowledge_chunk_tenant_document_idx ON knowledge_chunk (tenant_id, document_id);
CREATE INDEX IF NOT EXISTS knowledge_chunk_bm25_idx ON knowledge_chunk
    USING bm25 (chunk_id, content, (tenant_id::pdb.literal)) WITH (key_field = 'chunk_id');
