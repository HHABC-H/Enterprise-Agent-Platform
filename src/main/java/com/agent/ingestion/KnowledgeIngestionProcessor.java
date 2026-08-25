/**
 * 本文件定义 {@code KnowledgeIngestionProcessor}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

import com.agent.document.DocumentIndex;
import com.agent.document.DocumentService;
import com.agent.extension.GraphRelationSearchPort;
import com.agent.metrics.PlatformMetrics;
import com.agent.api.TraceIdHolder;
import org.springframework.stereotype.Service;

/** 统一执行幂等重建，消息适配器只负责传输。 */
@Service
public class KnowledgeIngestionProcessor {
    private final DocumentRevisionStore revisions;
    private final IngestionTaskStore tasks;
    private final DocumentService documents;
    private final DocumentIndexStore retrievalIndex;
    private final GraphRelationSearchPort graph;
    private final PlatformMetrics metrics;
    public KnowledgeIngestionProcessor(DocumentRevisionStore revisions, IngestionTaskStore tasks, DocumentService documents,
                                      DocumentIndexStore retrievalIndex, GraphRelationSearchPort graph, PlatformMetrics metrics) {
        this.revisions = revisions; this.tasks = tasks; this.documents = documents; this.retrievalIndex = retrievalIndex; this.graph = graph; this.metrics = metrics;
    }

    /** 在消费者线程中继承事件追踪标识，并在处理结束后恢复原线程上下文。 */
    public void process(KnowledgeDocumentChangedEvent event) {
        String previousTraceId = TraceIdHolder.get();
        TraceIdHolder.set(event.traceId() == null || event.traceId().isBlank() ? event.eventId() : event.traceId());
        try {
            processWithTrace(event);
        } finally {
            if (previousTraceId == null) { TraceIdHolder.clear(); } else { TraceIdHolder.set(previousTraceId); }
        }
    }

    /** 以任务仓储的幂等锁保护重建流程；最多重试三次，最终失败交由消息适配器处理。 */
    private void processWithTrace(KnowledgeDocumentChangedEvent event) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (!tasks.tryStart(event)) { metrics.recordIngestionSkipped(); return; }
            metrics.recordIngestion();
            try {
                metrics.recordIngestionDuration(() -> {
                DocumentRevision revision = revisions.find(event.tenantId(), event.documentId())
                        .filter(item -> item.contentHash().equals(event.contentHash()) && item.metadata().version().equals(event.version()))
                        .orElseThrow(() -> new IllegalArgumentException("未找到匹配的当前文档版本。"));
                if (event.operation() == DocumentOperation.DELETE) {
                    documents.delete(event.documentId());
                    retrievalIndex.deleteDocument(event.tenantId(), event.documentId());
                    graph.deleteDocument(event.tenantId(), event.documentId());
                }
                else {
                    DocumentIndex index = documents.rebuild(event.documentId(), revision.markdown(), revision.metadata());
                    retrievalIndex.replaceDocument(event.tenantId(), event.documentId(), event.version(), index);
                    graph.replaceDocument(event.tenantId(), event.documentId(), event.version(), index);
                }
                tasks.markSuccess(event); metrics.recordIngestionSuccess();
                return null;
                });
                return;
            } catch (RuntimeException exception) {
                boolean finalFailure = attempt == 3;
                tasks.markFailure(event, safeReason(exception), finalFailure);
                if (finalFailure) { metrics.recordIngestionFailure(); throw exception; }
                metrics.recordIngestionRetry();
            }
        }
    }

    /** 截断失败原因，防止异常信息被无界写入任务记录或指标系统。 */
    private String safeReason(RuntimeException exception) {
        String value = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return value.length() > 240 ? value.substring(0, 240) : value;
    }
}
