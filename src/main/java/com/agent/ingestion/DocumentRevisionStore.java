/**
 * 本文件定义 {@code DocumentRevisionStore}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

import java.util.Optional;

public interface DocumentRevisionStore {
    Optional<DocumentRevision> find(String tenantId, String documentId);
    void save(DocumentRevision revision);
}
