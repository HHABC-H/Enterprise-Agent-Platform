/**
 * 本文件定义 {@code InMemoryIngestionTaskStore}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.ingestion;

import com.agent.ingestion.IngestionState;
import com.agent.ingestion.IngestionTaskStatus;
import com.agent.ingestion.IngestionTaskStore;
import com.agent.ingestion.KnowledgeDocumentChangedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryIngestionTaskStore implements IngestionTaskStore {
    private final ConcurrentHashMap<String, IngestionTaskStatus> events = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> latestByDocument = new ConcurrentHashMap<>();
    private final Clock clock;
    public InMemoryIngestionTaskStore(Clock clock) { this.clock = clock; }
    @Override public void enqueue(KnowledgeDocumentChangedEvent event) {
        events.putIfAbsent(event.idempotencyKey(), new IngestionTaskStatus(event.eventId(), event.tenantId(), event.documentId(), event.version(),
                IngestionState.QUEUED, 0, null, Instant.now(clock), event.traceId()));
        latestByDocument.put(key(event), event.idempotencyKey());
    }
    @Override public boolean tryStart(KnowledgeDocumentChangedEvent event) {
        AtomicBoolean accepted = new AtomicBoolean();
        events.computeIfPresent(event.idempotencyKey(), (ignored, old) -> {
            if (old.state() == IngestionState.QUEUED) {
                accepted.set(true);
                return new IngestionTaskStatus(old.eventId(), old.tenantId(), old.documentId(), old.version(),
                        IngestionState.PROCESSING, old.attempts() + 1, null, Instant.now(clock), old.traceId());
            }
            return old;
        });
        if (accepted.get()) { latestByDocument.put(key(event), event.idempotencyKey()); }
        return accepted.get();
    }
    @Override public void markSuccess(KnowledgeDocumentChangedEvent event) { replace(event, IngestionState.SUCCESS, null); }
    @Override public void markFailure(KnowledgeDocumentChangedEvent event, String reason, boolean finalFailure) {
        replace(event, finalFailure ? IngestionState.FAILED : IngestionState.QUEUED, reason);
    }
    @Override public void markSkipped(KnowledgeDocumentChangedEvent event) {
        events.putIfAbsent(event.idempotencyKey(), new IngestionTaskStatus(event.eventId(), event.tenantId(), event.documentId(), event.version(),
                IngestionState.SKIPPED, 0, null, Instant.now(clock), event.traceId()));
        latestByDocument.put(key(event), event.idempotencyKey());
    }
    @Override public Optional<IngestionTaskStatus> findLatest(String tenantId, String documentId) {
        return Optional.ofNullable(latestByDocument.get(tenantId + ":" + documentId)).map(events::get);
    }
    private void replace(KnowledgeDocumentChangedEvent event, IngestionState state, String reason) {
        events.computeIfPresent(event.idempotencyKey(), (ignored, old) -> new IngestionTaskStatus(old.eventId(), old.tenantId(), old.documentId(),
                old.version(), state, old.attempts(), reason, Instant.now(clock), old.traceId()));
    }
    private String key(KnowledgeDocumentChangedEvent event) { return event.tenantId() + ":" + event.documentId(); }
}
