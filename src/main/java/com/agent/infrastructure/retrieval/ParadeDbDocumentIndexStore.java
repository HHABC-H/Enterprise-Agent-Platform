/**
 * 本文件定义 {@code ParadeDbDocumentIndexStore}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.retrieval;

import com.agent.document.Chunk;
import com.agent.document.DocumentIndex;
import com.agent.ingestion.DocumentIndexStore;
import java.sql.Array;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** ParadeDB 的批量全文索引写入适配器，所有数据均通过绑定参数传递。 */
@Component
@Profile("docker")
public class ParadeDbDocumentIndexStore implements DocumentIndexStore {
    private final NamedParameterJdbcTemplate jdbc;
    public ParadeDbDocumentIndexStore(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void replaceDocument(String tenantId, String documentId, String version, DocumentIndex index) {
        deleteDocument(tenantId, documentId);
        String sql = "INSERT INTO knowledge_chunk (chunk_id, parent_chunk_id, tenant_id, document_id, version, content, permission_tags, allowed_user_ids) "
                + "VALUES (:chunkId, :parentChunkId, :tenantId, :documentId, :version, :content, CAST(:permissionTags AS text[]), CAST(:allowedUserIds AS text[]))";
        jdbc.batchUpdate(sql, index.chunks().stream().map(chunk -> parameters(tenantId, documentId, version, chunk)).toArray(Map[]::new));
    }

    @Override
    public void deleteDocument(String tenantId, String documentId) {
        jdbc.update("DELETE FROM knowledge_chunk WHERE tenant_id = :tenantId AND document_id = :documentId",
                Map.of("tenantId", tenantId, "documentId", documentId));
    }

    private Map<String, Object> parameters(String tenantId, String documentId, String version, Chunk chunk) {
        Map<String, Object> values = new HashMap<>();
        values.put("chunkId", chunk.chunkId());
        values.put("parentChunkId", chunk.parentChunkId());
        values.put("tenantId", tenantId);
        values.put("documentId", documentId);
        values.put("version", version);
        values.put("content", chunk.content());
        values.put("permissionTags", postgresArray(chunk.metadata().permissionTags()));
        values.put("allowedUserIds", postgresArray(chunk.metadata().allowedUserIds()));
        return values;
    }

    private String postgresArray(java.util.Set<String> values) {
        return "{" + values.stream().map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(",")) + "}";
    }
}
