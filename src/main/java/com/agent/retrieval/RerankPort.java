package com.agent.retrieval;

import java.util.List;

public interface RerankPort {
    List<RetrievalEvidence> rerank(String question, List<RetrievalEvidence> evidence);
}
