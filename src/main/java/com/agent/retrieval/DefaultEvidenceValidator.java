package com.agent.retrieval;

import com.agent.config.AgentPlatformProperties;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DefaultEvidenceValidator implements EvidenceValidator {
    private final AgentPlatformProperties properties;
    public DefaultEvidenceValidator(AgentPlatformProperties properties) { this.properties = properties; }

    @Override
    public EvidenceDecision validate(String question, List<RetrievalEvidence> evidence) {
        if (evidence.isEmpty()) {
            return EvidenceDecision.refused("没有检索到你有权限访问的相关证据。");
        }
        RetrievalEvidence top = evidence.get(0);
        if (top.score() < properties.getRetrieval().getMinimumRrfScore()
                || TextTokens.overlapRatio(question, top.chunk().content()) < properties.getRetrieval().getMinimumTokenOverlap()) {
            return EvidenceDecision.refused("已检索到内容，但证据相关性不足，无法可靠回答。");
        }
        return EvidenceDecision.accepted();
    }
}
