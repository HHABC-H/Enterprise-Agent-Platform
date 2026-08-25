/**
 * 本文件定义 {@code IngestionTaskStore}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

import java.util.Optional;

public interface IngestionTaskStore {
    boolean tryStart(KnowledgeDocumentChangedEvent event);
    void markSuccess(KnowledgeDocumentChangedEvent event);
    void markFailure(KnowledgeDocumentChangedEvent event, String reason, boolean finalFailure);
    void markSkipped(KnowledgeDocumentChangedEvent event);
    Optional<IngestionTaskStatus> findLatest(String tenantId, String documentId);
}
