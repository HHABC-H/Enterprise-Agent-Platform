/**
 * 本文件定义 {@code HybridSearchPipeline}，负责检索、权限过滤、证据校验与排序流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.retrieval;

import com.agent.config.AgentPlatformProperties;
import com.agent.document.Chunk;
import com.agent.document.DocumentChunkStore;
import com.agent.metrics.PlatformMetrics;
import com.agent.ingestion.LocalDocumentIndexMirror;
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
    private final LocalDocumentIndexMirror localMirror;

    public HybridSearchPipeline(DocumentChunkStore store, SecurityFilter securityFilter, QueryRewriter queryRewriter,
                                VectorSearchPort vectorSearchPort, FullTextSearchPort fullTextSearchPort,
                                RerankPort rerankPort, EvidenceValidator evidenceValidator,
                                AgentPlatformProperties properties, Executor retrievalExecutor, PlatformMetrics metrics,
                                LocalDocumentIndexMirror localMirror) {
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
        this.localMirror = localMirror;
    }

    /** 将完整检索链路纳入耗时指标，避免计时逻辑散落到各适配器。 */
    @Override
    public SearchResponse search(String tenantId, String userId, String question) {
        return metrics.recordSearch(() -> doSearch(tenantId, userId, question));
    }

    /**
     * 合并持久化与本地镜像索引，先执行权限过滤，再并行检索、RRF 融合、可选重排和证据校验。
     */
    private SearchResponse doSearch(String tenantId, String userId, String question) {
        List<Chunk> indexed = new java.util.ArrayList<>(store.findAll());
        localMirror.findAll().forEach(chunk -> {
            if (indexed.stream().noneMatch(existing -> existing.chunkId().equals(chunk.chunkId()))) { indexed.add(chunk); }
        });
        List<Chunk> authorized = securityFilter.filter(tenantId, userId, indexed);
        String rewritten = properties.getRetrieval().isQueryRewriteEnabled() ? queryRewriter.rewrite(question) : question;
        int candidateLimit = properties.getRetrieval().getCandidateLimit();
        CompletableFuture<List<SearchCandidate>> vector = CompletableFuture.supplyAsync(
                () -> vectorSearchPort.search(rewritten, authorized, candidateLimit), retrievalExecutor);
        CompletableFuture<List<SearchCandidate>> fullText = CompletableFuture.supplyAsync(
                () -> fullTextSearchPort.search(rewritten, authorized, candidateLimit), retrievalExecutor);
        List<RetrievalEvidence> evidence = RrfFusion.fuse(vector.join(), fullText.join(), RRF_K).stream()
                .limit(5).map(this::withParentContext).toList();
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

    /** 子块 ID 用于证据定位，父块正文仅在元数据一致时作为回答上下文。 */
    private RetrievalEvidence withParentContext(RetrievalEvidence evidence) {
        Chunk child = evidence.chunk();
        if (child.parentChunkId() == null) { return evidence; }
        return store.findParent(child.parentChunkId())
                .filter(parent -> parent.documentId().equals(child.documentId()))
                .filter(parent -> parent.metadata().tenantId().equals(child.metadata().tenantId()))
                .map(parent -> new RetrievalEvidence(new Chunk(child.chunkId(), child.documentId(), parent.content(), child.headingPath(), child.metadata(), child.parentChunkId()),
                        evidence.score(), evidence.sources()))
                .orElse(evidence);
    }

}
