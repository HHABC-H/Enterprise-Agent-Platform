/**
 * 本文件定义 {@code InMemoryDocumentRevisionStore}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.ingestion;

import com.agent.ingestion.DocumentRevision;
import com.agent.ingestion.DocumentRevisionStore;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryDocumentRevisionStore implements DocumentRevisionStore {
    private final ConcurrentHashMap<String, DocumentRevision> revisions = new ConcurrentHashMap<>();
    @Override public Optional<DocumentRevision> find(String tenantId, String documentId) {
        return revisions.values().stream()
                .filter(item -> item.tenantId().equals(tenantId) && item.documentId().equals(documentId))
                .max(java.util.Comparator.comparing(DocumentRevision::updatedAt));
    }
    @Override public Optional<DocumentRevision> find(String tenantId, String documentId, String version) {
        return Optional.ofNullable(revisions.get(key(tenantId, documentId, version)));
    }
    @Override public void save(DocumentRevision revision) {
        revisions.put(key(revision.tenantId(), revision.documentId(), revision.metadata().version()), revision);
    }
    private String key(String tenantId, String documentId, String version) { return tenantId + ":" + documentId + ":" + version; }
}
