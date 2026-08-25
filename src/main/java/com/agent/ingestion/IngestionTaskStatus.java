/**
 * 本文件定义 {@code IngestionTaskStatus}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

import java.time.Instant;

/** 不暴露正文的入库任务状态。 */
public record IngestionTaskStatus(String eventId, String tenantId, String documentId, String version,
                                  IngestionState state, int attempts, String failureReason, Instant updatedAt,
                                  String traceId) { }
