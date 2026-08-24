package com.agent.retrieval;

import com.agent.document.Chunk;
import java.util.Set;

public record RetrievalEvidence(Chunk chunk, double score, Set<SearchSource> sources) {

    public RetrievalEvidence {
        sources = Set.copyOf(sources);
    }
}
