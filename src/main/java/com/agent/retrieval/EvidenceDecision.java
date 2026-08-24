package com.agent.retrieval;

public record EvidenceDecision(boolean sufficient, String refusalReason) {

    public static EvidenceDecision accepted() {
        return new EvidenceDecision(true, null);
    }

    public static EvidenceDecision refused(String reason) {
        return new EvidenceDecision(false, reason);
    }
}
