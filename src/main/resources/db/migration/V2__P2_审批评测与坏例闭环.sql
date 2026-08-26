CREATE TABLE workflow_checkpoint (
    workflow_id TEXT PRIMARY KEY, tenant_id TEXT NOT NULL, owner_user_id TEXT NOT NULL, session_id TEXT NOT NULL,
    input_summary TEXT NOT NULL, pending_action TEXT NOT NULL, state TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL, trace_id TEXT, row_version BIGINT NOT NULL,
    approver_id TEXT, decision TEXT, approval_comment TEXT, state_trace TEXT NOT NULL
);
CREATE INDEX workflow_checkpoint_pending_idx ON workflow_checkpoint (tenant_id, state, expires_at);

CREATE TABLE evaluation_dataset_version (
    id TEXT PRIMARY KEY, tenant_id TEXT NOT NULL, dataset_id TEXT NOT NULL, version INTEGER NOT NULL, name TEXT NOT NULL,
    created_by TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, row_version BIGINT NOT NULL,
    UNIQUE (tenant_id, dataset_id, version)
);
CREATE TABLE evaluation_sample (
    id TEXT PRIMARY KEY, tenant_id TEXT NOT NULL, dataset_version_id TEXT NOT NULL REFERENCES evaluation_dataset_version(id),
    question TEXT NOT NULL, expected_answer TEXT, sample_type TEXT NOT NULL, expected_evidence TEXT, expect_reject BOOLEAN NOT NULL,
    tags TEXT[] NOT NULL DEFAULT '{}', fingerprint CHAR(64) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL, UNIQUE (tenant_id, dataset_version_id, fingerprint)
);
CREATE TABLE evaluation_run (
    id TEXT PRIMARY KEY, tenant_id TEXT NOT NULL, dataset_version_id TEXT NOT NULL REFERENCES evaluation_dataset_version(id),
    state TEXT NOT NULL, requested_by TEXT NOT NULL, code_version TEXT NOT NULL, config_version TEXT NOT NULL,
    model_name TEXT, prompt_hash TEXT, started_at TIMESTAMPTZ, finished_at TIMESTAMPTZ, lease_until TIMESTAMPTZ,
    failure_category TEXT, summary_json TEXT, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, row_version BIGINT NOT NULL
);
CREATE INDEX evaluation_run_queue_idx ON evaluation_run (tenant_id, state, created_at);
CREATE TABLE evaluation_case_result (
    id TEXT PRIMARY KEY, tenant_id TEXT NOT NULL, run_id TEXT NOT NULL REFERENCES evaluation_run(id), sample_id TEXT NOT NULL,
    passed BOOLEAN NOT NULL, failure_category TEXT, duration_ms DOUBLE PRECISION NOT NULL, evidence_hit BOOLEAN NOT NULL,
    refused BOOLEAN NOT NULL, ragas_status TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL, UNIQUE (tenant_id, run_id, sample_id)
);
CREATE TABLE bad_case (
    id TEXT PRIMARY KEY, tenant_id TEXT NOT NULL, source_run_id TEXT NOT NULL, source_sample_id TEXT NOT NULL,
    stable_key CHAR(64) NOT NULL, failure_category TEXT NOT NULL, severity TEXT NOT NULL, status TEXT NOT NULL,
    owner_note TEXT, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, row_version BIGINT NOT NULL,
    UNIQUE (tenant_id, stable_key)
);
CREATE INDEX bad_case_filter_idx ON bad_case (tenant_id, status, failure_category, updated_at DESC);
CREATE TABLE platform_audit_log (
    id TEXT PRIMARY KEY, tenant_id TEXT NOT NULL, actor_id TEXT NOT NULL, operation TEXT NOT NULL, target_type TEXT NOT NULL,
    target_id TEXT NOT NULL, trace_id TEXT, summary TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
