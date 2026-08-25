package com.agent.infrastructure.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agent.document.Chunk;
import com.agent.document.DocumentIndex;
import com.agent.document.DocumentMetadata;
import com.agent.document.ParentChunk;
import com.agent.extension.GraphRelationQuery;
import com.agent.retrieval.SecurityFilter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InMemoryGraphRelationSearchAdapterTest {
    @Test
    void 跨租户无权限和超跳数均不能读取受限图关系() {
        DocumentMetadata privateMetadata = new DocumentMetadata("tenant-a", "内部", "v1", Set.of("user:alice"), Set.of());
        ParentChunk parent = new ParentChunk("p1", "doc", "内部内容", List.of("内部"), privateMetadata);
        Chunk child = new Chunk("c1", "doc", "内部内容 Service", List.of("内部"), privateMetadata, "p1");
        InMemoryGraphRelationSearchAdapter adapter = new InMemoryGraphRelationSearchAdapter(new SecurityFilter());
        adapter.replaceDocument("tenant-a", "doc", "v1", new DocumentIndex("doc", List.of(parent), List.of(child)));

        assertThat(adapter.search(new GraphRelationQuery("tenant-a", "bob", "doc", "v1", 3, 20))).isEmpty();
        assertThat(adapter.search(new GraphRelationQuery("tenant-b", "alice", "doc", "v1", 3, 20))).isEmpty();
        assertThatThrownBy(() -> new GraphRelationQuery("tenant-a", "alice", "doc", "v1", 4, 20)).isInstanceOf(IllegalArgumentException.class);
    }
}
