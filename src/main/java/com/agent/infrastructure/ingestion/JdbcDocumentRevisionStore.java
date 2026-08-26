/**
 * 本文件定义 {@code JdbcDocumentRevisionStore}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.ingestion;

import com.agent.document.DocumentMetadata;
import com.agent.ingestion.DocumentRevision;
import com.agent.ingestion.DocumentRevisionStore;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** docker profile 的当前文档版本仓储，正文只用于受控的入库处理流程。 */
@Component
@Profile({"docker", "local-docker"})
public class JdbcDocumentRevisionStore implements DocumentRevisionStore {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcDocumentRevisionStore(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public Optional<DocumentRevision> find(String tenantId, String documentId) {
        String sql = "SELECT tenant_id, document_id, markdown, source, version, permission_tags, allowed_user_ids, content_hash, updated_at "
                + "FROM knowledge_document_revision WHERE tenant_id = :tenantId AND document_id = :documentId ORDER BY updated_at DESC LIMIT 1";
        return queryOne(sql, Map.of("tenantId", tenantId, "documentId", documentId));
    }
    @Override public Optional<DocumentRevision> find(String tenantId, String documentId, String version) {
        String sql = "SELECT tenant_id, document_id, markdown, source, version, permission_tags, allowed_user_ids, content_hash, updated_at "
                + "FROM knowledge_document_revision WHERE tenant_id = :tenantId AND document_id = :documentId AND version = :version";
        return queryOne(sql, Map.of("tenantId", tenantId, "documentId", documentId, "version", version));
    }
    private Optional<DocumentRevision> queryOne(String sql, Map<String, ?> values) {
        return jdbc.query(sql, values, (resultSet, row) -> new DocumentRevision(
                resultSet.getString("tenant_id"), resultSet.getString("document_id"), resultSet.getString("markdown"),
                new DocumentMetadata(resultSet.getString("tenant_id"), resultSet.getString("source"), resultSet.getString("version"),
                        sqlArray(resultSet.getArray("permission_tags")), sqlArray(resultSet.getArray("allowed_user_ids"))),
                resultSet.getString("content_hash"), resultSet.getTimestamp("updated_at").toInstant())).stream().findFirst();
    }
    @Override public void save(DocumentRevision revision) {
        String sql = "INSERT INTO knowledge_document_revision (tenant_id, document_id, markdown, source, version, permission_tags, allowed_user_ids, content_hash, updated_at) "
                + "VALUES (:tenantId, :documentId, :markdown, :source, :version, CAST(:permissionTags AS text[]), CAST(:allowedUserIds AS text[]), :contentHash, :updatedAt) "
                + "ON CONFLICT (tenant_id, document_id, version) DO UPDATE SET markdown = EXCLUDED.markdown, source = EXCLUDED.source, "
                + "permission_tags = EXCLUDED.permission_tags, allowed_user_ids = EXCLUDED.allowed_user_ids, content_hash = EXCLUDED.content_hash, updated_at = EXCLUDED.updated_at";
        jdbc.update(sql, Map.of("tenantId", revision.tenantId(), "documentId", revision.documentId(), "markdown", revision.markdown(),
                "source", revision.metadata().source(), "version", revision.metadata().version(), "permissionTags", postgresArray(revision.metadata().permissionTags()),
                "allowedUserIds", postgresArray(revision.metadata().allowedUserIds()), "contentHash", revision.contentHash(), "updatedAt", Timestamp.from(revision.updatedAt())));
    }
    private java.util.Set<String> sqlArray(java.sql.Array value) throws java.sql.SQLException {
        if (value == null) { return java.util.Set.of(); }
        Object raw = value.getArray();
        if (raw instanceof String[] items) { return java.util.Set.of(items); }
        return java.util.Set.of();
    }
    private String postgresArray(java.util.Set<String> values) {
        return "{" + values.stream().map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(",")) + "}";
    }
}
