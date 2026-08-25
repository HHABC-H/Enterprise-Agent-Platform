/**
 * 本文件定义 {@code InMemoryVectorSearchAdapter}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.retrieval;

import com.agent.document.Chunk;
import com.agent.retrieval.EmbeddingService;
import com.agent.retrieval.SearchCandidate;
import com.agent.retrieval.SearchSource;
import com.agent.retrieval.VectorSearchPort;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InMemoryVectorSearchAdapter implements VectorSearchPort {
    private final EmbeddingService embeddingService;
    public InMemoryVectorSearchAdapter(EmbeddingService embeddingService) { this.embeddingService = embeddingService; }
    @Override
    public List<SearchCandidate> search(String question, List<Chunk> authorizedChunks, int limit) {
        return authorizedChunks.stream()
                .map(chunk -> new SearchCandidate(chunk, embeddingService.similarity(question, chunk.content()), SearchSource.VECTOR))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingDouble(SearchCandidate::score).reversed())
                .limit(limit).toList();
    }
}
