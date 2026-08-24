package com.agent.retrieval;

import com.agent.document.Chunk;
import java.util.List;

public interface FullTextSearchPort {
    List<SearchCandidate> search(String question, List<Chunk> authorizedChunks, int limit);
}
