/**
 * 本文件定义 {@code KnowledgeDocumentChangedEvent}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

import java.time.Instant;

/** 供本地发布器或 RabbitMQ 传输的版本化文档事件。 */
public record KnowledgeDocumentChangedEvent(String eventId, String tenantId, String documentId, String version,
                                            String contentHash, DocumentOperation operation, Instant occurredAt,
                                            String traceId, String idempotencyKey) { }
