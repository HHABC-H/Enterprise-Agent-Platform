package com.agent.api;

import com.agent.chat.ChatResult;
import com.agent.workflow.WorkflowState;
import java.util.List;

public record ChatResponse(String answer, boolean refused, String refusalReason, List<EvidenceResponse> evidence,
                           List<WorkflowState> trace) {
    public static ChatResponse from(ChatResult result) {
        return new ChatResponse(result.answer(), result.refused(), result.refusalReason(),
                result.evidence().stream().map(EvidenceResponse::from).toList(), result.trace());
    }
}
