/**
 * 本文件定义 {@code JdbcIngestionTaskStore}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.ingestion;

import com.agent.ingestion.IngestionState;
import com.agent.ingestion.IngestionTaskStatus;
import com.agent.ingestion.IngestionTaskStore;
import com.agent.ingestion.KnowledgeDocumentChangedEvent;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** 基于唯一幂等键的 PostgreSQL 入库任务仓储。 */
@Component
@Profile({"docker", "local-docker"})
public class JdbcIngestionTaskStore implements IngestionTaskStore {
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;
    public JdbcIngestionTaskStore(NamedParameterJdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    @Override public void enqueue(KnowledgeDocumentChangedEvent event) {
        String sql = "INSERT INTO knowledge_ingestion_task (idempotency_key, event_id, tenant_id, document_id, version, state, attempts, failure_reason, updated_at, trace_id) "
                + "VALUES (:key, :eventId, :tenantId, :documentId, :version, 'QUEUED', 0, NULL, :updatedAt, :traceId) ON CONFLICT (idempotency_key) DO NOTHING";
        jdbc.update(sql, parameters(event));
    }
    @Override public boolean tryStart(KnowledgeDocumentChangedEvent event) {
        String sql = "INSERT INTO knowledge_ingestion_task (idempotency_key, event_id, tenant_id, document_id, version, state, attempts, failure_reason, updated_at, trace_id) "
                + "VALUES (:key, :eventId, :tenantId, :documentId, :version, 'PROCESSING', 1, NULL, :updatedAt, :traceId) "
                + "ON CONFLICT (idempotency_key) DO UPDATE SET state = 'PROCESSING', attempts = knowledge_ingestion_task.attempts + 1, failure_reason = NULL, updated_at = EXCLUDED.updated_at "
                + "WHERE knowledge_ingestion_task.state = 'QUEUED'";
        return jdbc.update(sql, parameters(event)) == 1;
    }
    @Override public void markSuccess(KnowledgeDocumentChangedEvent event) { update(event, IngestionState.SUCCESS, null); }
    @Override public void markFailure(KnowledgeDocumentChangedEvent event, String reason, boolean finalFailure) {
        update(event, finalFailure ? IngestionState.FAILED : IngestionState.QUEUED, reason);
    }
    @Override public void markSkipped(KnowledgeDocumentChangedEvent event) {
        String sql = "INSERT INTO knowledge_ingestion_task (idempotency_key, event_id, tenant_id, document_id, version, state, attempts, updated_at, trace_id) "
                + "VALUES (:key, :eventId, :tenantId, :documentId, :version, 'SKIPPED', 0, :updatedAt, :traceId) ON CONFLICT (idempotency_key) DO NOTHING";
        jdbc.update(sql, parameters(event));
    }
    @Override public Optional<IngestionTaskStatus> findLatest(String tenantId, String documentId) {
        String sql = "SELECT event_id, tenant_id, document_id, version, state, attempts, failure_reason, updated_at, trace_id FROM knowledge_ingestion_task "
                + "WHERE tenant_id = :tenantId AND document_id = :documentId ORDER BY updated_at DESC LIMIT 1";
        return jdbc.query(sql, Map.of("tenantId", tenantId, "documentId", documentId), (resultSet, row) -> new IngestionTaskStatus(
                resultSet.getString("event_id"), resultSet.getString("tenant_id"), resultSet.getString("document_id"), resultSet.getString("version"),
                IngestionState.valueOf(resultSet.getString("state")), resultSet.getInt("attempts"), resultSet.getString("failure_reason"),
                resultSet.getTimestamp("updated_at").toInstant(), resultSet.getString("trace_id"))).stream().findFirst();
    }
    private void update(KnowledgeDocumentChangedEvent event, IngestionState state, String reason) {
        jdbc.update("UPDATE knowledge_ingestion_task SET state = :state, failure_reason = :reason, updated_at = :updatedAt WHERE idempotency_key = :key",
                Map.of("state", state.name(), "reason", reason == null ? "" : reason, "updatedAt", now(), "key", event.idempotencyKey()));
    }
    private Map<String, Object> parameters(KnowledgeDocumentChangedEvent event) {
        return Map.of("key", event.idempotencyKey(), "eventId", event.eventId(), "tenantId", event.tenantId(), "documentId", event.documentId(),
                "version", event.version(), "updatedAt", now(), "traceId", event.traceId() == null ? "" : event.traceId());
    }
    private Timestamp now() { return Timestamp.from(Instant.now(clock)); }
}
