package com.agent.retrieval;

import com.agent.document.Chunk;
import java.util.List;

public interface VectorSearchPort {

    List<SearchCandidate> search(String question, List<Chunk> authorizedChunks, int limit);
}
