ALTER TABLE knowledge_document_revision DROP CONSTRAINT IF EXISTS knowledge_document_revision_pkey;
ALTER TABLE knowledge_document_revision ADD PRIMARY KEY (tenant_id, document_id, version);
CREATE INDEX IF NOT EXISTS knowledge_document_revision_latest_idx
    ON knowledge_document_revision (tenant_id, document_id, updated_at DESC);
