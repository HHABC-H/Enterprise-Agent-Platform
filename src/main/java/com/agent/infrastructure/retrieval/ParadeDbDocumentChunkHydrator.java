package com.agent.infrastructure.retrieval;

import com.agent.document.Chunk;
import com.agent.document.DocumentChunkStore;
import com.agent.document.DocumentMetadata;
import com.agent.ingestion.LocalDocumentIndexMirror;
import java.sql.Array;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 应用重启后将 ParadeDB 中的知识分块回灌到运行时索引，避免 RAG 仅因 JVM 内存清空而失效。
 */
@Component
@Profile({"docker", "local-docker"})
public class ParadeDbDocumentChunkHydrator {
    private static final Logger log = LoggerFactory.getLogger(ParadeDbDocumentChunkHydrator.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final DocumentChunkStore store;
    private final LocalDocumentIndexMirror localMirror;

    public ParadeDbDocumentChunkHydrator(NamedParameterJdbcTemplate jdbc, DocumentChunkStore store,
                                         LocalDocumentIndexMirror localMirror) {
        this.jdbc = jdbc;
        this.store = store;
        this.localMirror = localMirror;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void hydrate() {
        try {
            List<Chunk> chunks = jdbc.query("""
                    SELECT c.chunk_id, c.parent_chunk_id, c.tenant_id, c.document_id, c.version, c.content,
                           c.permission_tags, c.allowed_user_ids, COALESCE(r.source, c.document_id) AS source
                    FROM knowledge_chunk c
                    LEFT JOIN knowledge_document_revision r
                      ON r.tenant_id = c.tenant_id AND r.document_id = c.document_id AND r.version = c.version
                    """, (resultSet, rowNumber) -> new Chunk(
                    resultSet.getString("chunk_id"),
                    resultSet.getString("document_id"),
                    resultSet.getString("content"),
                    List.of(),
                    new DocumentMetadata(resultSet.getString("tenant_id"), resultSet.getString("source"),
                            resultSet.getString("version"), readSet(resultSet.getArray("permission_tags")),
                            readSet(resultSet.getArray("allowed_user_ids"))),
                    resultSet.getString("parent_chunk_id")));
            Map<String, List<Chunk>> byDocument = chunks.stream().collect(Collectors.groupingBy(Chunk::documentId));
            byDocument.forEach((documentId, documentChunks) -> {
                store.save(documentId, List.of(), documentChunks);
                localMirror.replace(documentId, documentChunks);
            });
            log.info("已从 ParadeDB 回灌 {} 个知识分块，覆盖 {} 个文档", chunks.size(), byDocument.size());
        } catch (DataAccessException exception) {
            log.warn("ParadeDB 知识分块回灌失败，当前进程无法检索重启前已入库文档: {}", exception.getClass().getSimpleName());
        }
    }

    private static Set<String> readSet(Array array) throws SQLException {
        if (array == null || !(array.getArray() instanceof String[] values)) {
            return Set.of();
        }
        return Set.of(values);
    }
}
