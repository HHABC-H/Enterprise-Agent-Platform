package com.agent.retrieval;

import java.util.List;

public record SearchResponse(String rewrittenQuestion, List<RetrievalEvidence> evidence, EvidenceDecision decision) {

    public SearchResponse {
        evidence = List.copyOf(evidence);
    }
}
