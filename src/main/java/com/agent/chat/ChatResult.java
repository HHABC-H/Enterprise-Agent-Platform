package com.agent.chat;

import com.agent.retrieval.RetrievalEvidence;
import com.agent.workflow.WorkflowState;
import java.util.List;

public record ChatResult(String answer, boolean refused, String refusalReason, List<RetrievalEvidence> evidence,
                         List<WorkflowState> trace) {
    public ChatResult { evidence = List.copyOf(evidence); trace = List.copyOf(trace); }
}
