package com.agent.retrieval;

import java.util.List;

public interface EvidenceValidator {
    EvidenceDecision validate(String question, List<RetrievalEvidence> evidence);
}
