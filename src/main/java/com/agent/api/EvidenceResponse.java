package com.agent.api;

import com.agent.retrieval.RetrievalEvidence;
import java.util.Set;

public record EvidenceResponse(String documentId, String chunkId, String source, double score, Set<String> channels) {
    public static EvidenceResponse from(RetrievalEvidence evidence) {
        return new EvidenceResponse(evidence.chunk().documentId(), evidence.chunk().chunkId(), evidence.chunk().metadata().source(),
                evidence.score(), evidence.sources().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
}
