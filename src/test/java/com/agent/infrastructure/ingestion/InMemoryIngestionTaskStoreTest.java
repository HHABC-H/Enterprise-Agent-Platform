package com.agent.infrastructure.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.ingestion.DocumentOperation;
import com.agent.ingestion.IngestionState;
import com.agent.ingestion.KnowledgeDocumentChangedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryIngestionTaskStoreTest {
    @Test
    void 任务入队后可立即查询状态() {
        InMemoryIngestionTaskStore store = new InMemoryIngestionTaskStore(Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC));
        KnowledgeDocumentChangedEvent queued = event("queued");

        store.enqueue(queued);

        assertThat(store.findLatest("tenant-a", "doc-queued").orElseThrow().state()).isEqualTo(IngestionState.QUEUED);
        assertThat(store.findLatest("tenant-a", "doc-queued").orElseThrow().attempts()).isZero();
    }

    @Test
    void 相同事件只处理一次且失败最多尝试三次() {
        InMemoryIngestionTaskStore store = new InMemoryIngestionTaskStore(Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC));
        KnowledgeDocumentChangedEvent once = event("same");
        assertThat(store.tryStart(once)).isTrue();
        store.markSuccess(once);
        assertThat(store.tryStart(once)).isFalse();

        KnowledgeDocumentChangedEvent retry = event("retry");
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(store.tryStart(retry)).isTrue();
            store.markFailure(retry, "临时错误", attempt == 3);
        }
        assertThat(store.findLatest("tenant-a", "doc-retry").orElseThrow().state()).isEqualTo(IngestionState.FAILED);
        assertThat(store.findLatest("tenant-a", "doc-retry").orElseThrow().attempts()).isEqualTo(3);
    }
    private KnowledgeDocumentChangedEvent event(String key) {
        return new KnowledgeDocumentChangedEvent(key, "tenant-a", "doc-" + key, "v1", "hash", DocumentOperation.UPSERT,
                Instant.parse("2026-08-24T00:00:00Z"), "trace", key);
    }
}
