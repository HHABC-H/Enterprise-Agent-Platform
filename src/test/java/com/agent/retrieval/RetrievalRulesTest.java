package com.agent.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.config.AgentPlatformProperties;
import com.agent.document.Chunk;
import com.agent.document.DocumentMetadata;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RetrievalRulesTest {
    private final DocumentMetadata publicMetadata = new DocumentMetadata("tenant-a", "手册", "v1", Set.of("public"), Set.of());

    @Test
    void shouldRemoveUnauthorizedChunksBeforeFusion() {
        Chunk visible = new Chunk("visible", "doc1", "可见内容", List.of(), publicMetadata);
        Chunk secret = new Chunk("secret", "doc2", "机密内容", List.of(), new DocumentMetadata("tenant-a", "机密", "v1", Set.of("user:alice"), Set.of()));
        Chunk otherTenant = new Chunk("other", "doc3", "其他租户", List.of(), new DocumentMetadata("tenant-b", "其他", "v1", Set.of("public"), Set.of()));
        assertThat(new SecurityFilter().filter("tenant-a", "bob", List.of(visible, secret, otherTenant))).containsExactly(visible);
    }

    @Test
    void shouldUseRrfToPromoteCandidatesSeenByBothChannels() {
        Chunk first = new Chunk("first", "doc1", "内容", List.of(), publicMetadata);
        Chunk shared = new Chunk("shared", "doc2", "内容", List.of(), publicMetadata);
        var result = RrfFusion.fuse(List.of(new SearchCandidate(first, 0.9, SearchSource.VECTOR), new SearchCandidate(shared, 0.8, SearchSource.VECTOR)),
                List.of(new SearchCandidate(shared, 0.9, SearchSource.FULL_TEXT)), 60);
        assertThat(result.get(0).chunk().chunkId()).isEqualTo("shared");
        assertThat(result.get(0).sources()).containsExactlyInAnyOrder(SearchSource.VECTOR, SearchSource.FULL_TEXT);
    }

    @Test
    void shouldRefuseLowConfidenceEvidence() {
        AgentPlatformProperties properties = new AgentPlatformProperties();
        DefaultEvidenceValidator validator = new DefaultEvidenceValidator(properties);
        Chunk chunk = new Chunk("c", "d", "与问题无关", List.of(), publicMetadata);
        EvidenceDecision decision = validator.validate("平台支持什么", List.of(new RetrievalEvidence(chunk, 0.001, Set.of(SearchSource.VECTOR))));
        assertThat(decision.sufficient()).isFalse();
        assertThat(decision.refusalReason()).contains("相关性不足");
    }
}
