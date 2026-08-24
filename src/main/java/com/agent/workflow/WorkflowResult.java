package com.agent.workflow;

import com.agent.retrieval.SearchResponse;
import java.util.List;

public record WorkflowResult(SearchResponse searchResponse, List<WorkflowState> trace) {
    public WorkflowResult { trace = List.copyOf(trace); }
}
