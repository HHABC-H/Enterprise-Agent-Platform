/**
 * 本文件定义 {@code DocumentIngestionFacade}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

import com.agent.api.TraceIdHolder;
import com.agent.document.DocumentChunkStore;
import com.agent.document.DocumentMetadata;
import com.agent.extension.KnowledgeIngestionEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** 接口层统一保存当前版本并发布重建事件。 */
@Service
public class DocumentIngestionFacade {
    private final DocumentRevisionStore revisions;
    private final IngestionTaskStore tasks;
    private final KnowledgeIngestionEventPublisher publisher;
    private final DocumentChunkStore chunks;
    private final LocalDocumentIndexMirror localMirror;
    private final Clock clock;
    public DocumentIngestionFacade(DocumentRevisionStore revisions, IngestionTaskStore tasks, KnowledgeIngestionEventPublisher publisher,
                                   DocumentChunkStore chunks, Clock clock, LocalDocumentIndexMirror localMirror) {
        this.revisions = revisions; this.tasks = tasks; this.publisher = publisher; this.chunks = chunks; this.clock = clock; this.localMirror = localMirror;
    }

    /**
     * 保存新版本并发布变更事件；内容哈希未变化时只标记跳过，避免重复切分和建索引。
     */
    public List<com.agent.document.Chunk> upsert(String documentId, String markdown, DocumentMetadata metadata) {
        String hash = ContentHashing.sha256(markdown);
        if (revisions.find(metadata.tenantId(), documentId).map(item -> item.contentHash().equals(hash)).orElse(false)) {
            KnowledgeDocumentChangedEvent skipped = event(documentId, metadata, hash);
            tasks.markSkipped(skipped);
            return chunks.findAll().stream().filter(item -> item.documentId().equals(documentId))
                    .filter(item -> item.metadata().tenantId().equals(metadata.tenantId())).toList();
        }
        revisions.save(new DocumentRevision(metadata.tenantId(), documentId, markdown, metadata, hash, Instant.now(clock)));
        publisher.publish(event(documentId, metadata, hash));
        List<com.agent.document.Chunk> result = chunks.findAll().stream().filter(item -> item.documentId().equals(documentId))
                .filter(item -> item.metadata().tenantId().equals(metadata.tenantId())).toList();
        localMirror.replace(documentId, result);
        return result;
    }

    /** 读取同一租户下该文档的最近入库状态，不暴露文档正文。 */
    public IngestionTaskStatus status(String tenantId, String documentId) {
        return tasks.findLatest(tenantId, documentId).orElseThrow(() -> new IllegalArgumentException("未找到该文档的入库任务。"));
    }

    /** 将追踪标识和幂等键固化到事件，确保消息适配器不会参与业务判断。 */
    private KnowledgeDocumentChangedEvent event(String documentId, DocumentMetadata metadata, String hash) {
        String eventId = UUID.randomUUID().toString();
        return new KnowledgeDocumentChangedEvent(eventId, metadata.tenantId(), documentId, metadata.version(), hash, DocumentOperation.UPSERT,
                Instant.now(clock), TraceIdHolder.get(), metadata.tenantId() + ":" + documentId + ":" + metadata.version() + ":" + hash);
    }
}
