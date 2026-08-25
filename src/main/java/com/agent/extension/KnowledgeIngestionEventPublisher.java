/**
 * 本文件定义 {@code KnowledgeIngestionEventPublisher}，负责面向外部能力的端口与领域模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.extension;

import com.agent.ingestion.KnowledgeDocumentChangedEvent;

/** 文档变更消息的传输端口。 */
public interface KnowledgeIngestionEventPublisher {
    void publish(KnowledgeDocumentChangedEvent event);
    default void publish(String documentId, String contentSha256, String version, String idempotencyKey) {
        throw new UnsupportedOperationException("请使用包含租户和追踪信息的文档变更事件。");
    }
}
