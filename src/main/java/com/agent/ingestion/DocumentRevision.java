/**
 * 本文件定义 {@code DocumentRevision}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

import com.agent.document.DocumentMetadata;
import java.time.Instant;

/** 入库消费者读取的当前文档版本；正文只保存在仓储，不进入日志或指标。 */
public record DocumentRevision(String tenantId, String documentId, String markdown, DocumentMetadata metadata,
                               String contentHash, Instant updatedAt) { }
