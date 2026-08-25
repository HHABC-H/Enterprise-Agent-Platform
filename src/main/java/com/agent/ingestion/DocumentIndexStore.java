/**
 * 本文件定义 {@code DocumentIndexStore}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

import com.agent.document.DocumentIndex;

/** 检索持久化端口；核心入库流程不依赖具体数据库客户端。 */
public interface DocumentIndexStore {
    void replaceDocument(String tenantId, String documentId, String version, DocumentIndex index);
    void deleteDocument(String tenantId, String documentId);
}
