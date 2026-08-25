package com.agent.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import com.agent.config.AgentPlatformProperties;
import com.agent.infrastructure.document.InMemoryDocumentChunkStore;
import com.agent.infrastructure.retrieval.HashEmbeddingService;
import com.agent.infrastructure.retrieval.IdentityQueryRewriter;
import com.agent.infrastructure.retrieval.IdentityRerankAdapter;
import com.agent.infrastructure.retrieval.InMemoryFullTextSearchAdapter;
import com.agent.infrastructure.retrieval.InMemoryVectorSearchAdapter;
import com.agent.metrics.PlatformMetrics;
import com.agent.retrieval.DefaultEvidenceValidator;
import com.agent.retrieval.HybridSearchPipeline;
import com.agent.retrieval.SecurityFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ParentChildIndexTest {
    @Test
    void 子块命中时能够取得同标题路径的父上下文() {
        DocumentMetadata metadata = new DocumentMetadata("tenant-a", "手册", "v1", Set.of("public"), Set.of());
        Chunk first = new Chunk("c1", "doc", "第一段", java.util.List.of("使用说明"), metadata);
        Chunk second = new Chunk("c2", "doc", "第二段", java.util.List.of("使用说明"), metadata);
        DocumentIndex index = new ParentChunkFactory().create("doc", java.util.List.of(first, second));

        Chunk child = index.chunks().get(0);
        ParentChunk parent = index.parents().stream().filter(item -> item.parentChunkId().equals(child.parentChunkId())).findFirst().orElseThrow();
        assertThat(parent.content()).contains("第一段", "第二段");
        assertThat(parent.metadata().tenantId()).isEqualTo("tenant-a");
    }

    @Test
    void 检索保留子块标识但将父块传给回答上下文() {
        DocumentMetadata metadata = new DocumentMetadata("tenant-context", "手册", "v1", Set.of("public"), Set.of());
        ParentChunk parent = new ParentChunk("parent-context", "doc-context", "第一段。\n\n第二段。", java.util.List.of("说明"), metadata);
        Chunk child = new Chunk("child-context", "doc-context", "第二段。", java.util.List.of("说明"), metadata, parent.parentChunkId());
        InMemoryDocumentChunkStore store = new InMemoryDocumentChunkStore();
        store.save("doc-context", java.util.List.of(parent), java.util.List.of(child));
        AgentPlatformProperties properties = new AgentPlatformProperties();
        HybridSearchPipeline pipeline = new HybridSearchPipeline(store, new SecurityFilter(), new IdentityQueryRewriter(), new InMemoryVectorSearchAdapter(new HashEmbeddingService()),
                new InMemoryFullTextSearchAdapter(), new IdentityRerankAdapter(), new DefaultEvidenceValidator(properties), properties,
                Runnable::run, new PlatformMetrics(new SimpleMeterRegistry()), new com.agent.ingestion.LocalDocumentIndexMirror());

        var evidence = pipeline.search("tenant-context", "user", "第二段").evidence().get(0);
        assertThat(evidence.chunk().chunkId()).isEqualTo("child-context");
        assertThat(evidence.chunk().content()).contains("第一段", "第二段");
    }
}
