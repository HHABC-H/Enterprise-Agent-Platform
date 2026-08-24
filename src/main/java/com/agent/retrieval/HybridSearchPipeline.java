package com.agent.retrieval;

import com.agent.config.AgentPlatformProperties;
import com.agent.document.Chunk;
import com.agent.document.DocumentChunkStore;
import com.agent.metrics.PlatformMetrics;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.stereotype.Service;

@Service
public class HybridSearchPipeline implements SearchPipeline {
    private static final int RRF_K = 60;
    private final DocumentChunkStore store;
    private final SecurityFilter securityFilter;
    private final QueryRewriter queryRewriter;
    private final VectorSearchPort vectorSearchPort;
    private final FullTextSearchPort fullTextSearchPort;
    private final RerankPort rerankPort;
    private final EvidenceValidator evidenceValidator;
    private final AgentPlatformProperties properties;
    private final Executor retrievalExecutor;
    private final PlatformMetrics metrics;

    public HybridSearchPipeline(DocumentChunkStore store, SecurityFilter securityFilter, QueryRewriter queryRewriter,
                                VectorSearchPort vectorSearchPort, FullTextSearchPort fullTextSearchPort,
                                RerankPort rerankPort, EvidenceValidator evidenceValidator,
                                AgentPlatformProperties properties, Executor retrievalExecutor, PlatformMetrics metrics) {
        this.store = store;
        this.securityFilter = securityFilter;
        this.queryRewriter = queryRewriter;
        this.vectorSearchPort = vectorSearchPort;
        this.fullTextSearchPort = fullTextSearchPort;
        this.rerankPort = rerankPort;
        this.evidenceValidator = evidenceValidator;
        this.properties = properties;
        this.retrievalExecutor = retrievalExecutor;
        this.metrics = metrics;
    }

    @Override
    public SearchResponse search(String tenantId, String userId, String question) {
        return metrics.recordSearch(() -> doSearch(tenantId, userId, question));
    }

    private SearchResponse doSearch(String tenantId, String userId, String question) {
        List<Chunk> authorized = securityFilter.filter(tenantId, userId, store.findAll());
        String rewritten = properties.getRetrieval().isQueryRewriteEnabled() ? queryRewriter.rewrite(question) : question;
        int candidateLimit = properties.getRetrieval().getCandidateLimit();
        CompletableFuture<List<SearchCandidate>> vector = CompletableFuture.supplyAsync(
                () -> vectorSearchPort.search(rewritten, authorized, candidateLimit), retrievalExecutor);
        CompletableFuture<List<SearchCandidate>> fullText = CompletableFuture.supplyAsync(
                () -> fullTextSearchPort.search(rewritten, authorized, candidateLimit), retrievalExecutor);
        List<RetrievalEvidence> evidence = RrfFusion.fuse(vector.join(), fullText.join(), RRF_K).stream().limit(5).toList();
        if (properties.getRetrieval().isRerankEnabled()) {
            evidence = rerankPort.rerank(rewritten, evidence).stream().limit(5).toList();
        }
        EvidenceDecision decision = evidenceValidator.validate(rewritten, evidence);
        metrics.recordHitCount(evidence.size());
        if (!decision.sufficient()) {
            metrics.recordRefusal();
        }
        return new SearchResponse(rewritten, evidence, decision);
    }

}
