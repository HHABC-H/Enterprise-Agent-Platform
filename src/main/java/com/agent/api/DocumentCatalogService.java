package com.agent.api;

import com.agent.ingestion.DocumentRevision;
import com.agent.ingestion.DocumentRevisionStore;
import com.agent.retrieval.SecurityFilter;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** 为图谱查询提供受现有文档权限规则约束的文档和版本选择项。 */
@Service
public class DocumentCatalogService {
    private final DocumentRevisionStore revisions;
    private final SecurityFilter securityFilter;

    public DocumentCatalogService(DocumentRevisionStore revisions, SecurityFilter securityFilter) {
        this.revisions = revisions;
        this.securityFilter = securityFilter;
    }

    public List<DocumentCatalogItem> documents(String tenantId, String userId) {
        return revisions.findAll(tenantId).stream()
                .filter(revision -> securityFilter.canAccess(userId, revision.metadata()))
                .collect(java.util.stream.Collectors.toMap(DocumentRevision::documentId, revision -> revision,
                        (left, right) -> left.updatedAt().isAfter(right.updatedAt()) ? left : right))
                .values().stream()
                .sorted(Comparator.comparing((DocumentRevision item) -> item.metadata().source())
                        .thenComparing(DocumentRevision::documentId))
                .map(item -> new DocumentCatalogItem(item.documentId(), item.metadata().source()))
                .toList();
    }

    public List<DocumentVersionItem> versions(String tenantId, String userId, String documentId) {
        return revisions.findAll(tenantId).stream()
                .filter(revision -> revision.documentId().equals(documentId))
                .filter(revision -> securityFilter.canAccess(userId, revision.metadata()))
                .sorted(Comparator.comparing(DocumentRevision::updatedAt))
                .map(revision -> new DocumentVersionItem(revision.metadata().version()))
                .toList();
    }
}
