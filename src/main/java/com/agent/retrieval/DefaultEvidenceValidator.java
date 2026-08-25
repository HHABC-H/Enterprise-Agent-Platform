/**
 * 本文件定义 {@code DefaultEvidenceValidator}，负责检索、权限过滤、证据校验与排序流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
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
