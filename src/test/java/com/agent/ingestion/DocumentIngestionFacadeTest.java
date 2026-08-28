package com.agent.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.document.DocumentMetadata;
import com.agent.extension.KnowledgeIngestionEventPublisher;
import com.agent.infrastructure.document.InMemoryDocumentChunkStore;
import com.agent.infrastructure.ingestion.InMemoryDocumentRevisionStore;
import com.agent.infrastructure.ingestion.InMemoryIngestionTaskStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DocumentIngestionFacadeTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-26T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void publishSeesQueuedTaskBeforeMessageIsSent() {
        InMemoryIngestionTaskStore tasks = new InMemoryIngestionTaskStore(CLOCK);
        AtomicReference<IngestionTaskStatus> statusAtPublish = new AtomicReference<>();
        KnowledgeIngestionEventPublisher publisher = event -> statusAtPublish.set(tasks.findLatest(event.tenantId(), event.documentId()).orElseThrow());
        DocumentIngestionFacade facade = new DocumentIngestionFacade(new InMemoryDocumentRevisionStore(), tasks, publisher,
                new InMemoryDocumentChunkStore(), CLOCK, new LocalDocumentIndexMirror());

        facade.upsert("doc-queued", "# 异步入库", new DocumentMetadata("tenant-a", "测试", "v1", Set.of("public"), Set.of()));

        assertThat(statusAtPublish.get()).isNotNull();
        assertThat(statusAtPublish.get().state()).isEqualTo(IngestionState.QUEUED);
        assertThat(statusAtPublish.get().attempts()).isZero();
        assertThat(facade.status("tenant-a", "doc-queued")).isEqualTo(statusAtPublish.get());
    }
}
