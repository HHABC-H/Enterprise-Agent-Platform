package com.agent.workflow;

import com.agent.config.AgentPlatformProperties;
import com.agent.metrics.PlatformMetrics;
import com.agent.retrieval.SearchPipeline;
import com.agent.retrieval.SearchResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentWorkflowService {
    private final SearchPipeline searchPipeline;
    private final AgentPlatformProperties properties;
    private final WorkflowCheckpointPort checkpointPort;
    private final PlatformMetrics metrics;
    public AgentWorkflowService(SearchPipeline searchPipeline, AgentPlatformProperties properties,
                                WorkflowCheckpointPort checkpointPort, PlatformMetrics metrics) {
        this.searchPipeline = searchPipeline;
        this.properties = properties;
        this.checkpointPort = checkpointPort;
        this.metrics = metrics;
    }
    public WorkflowResult execute(String tenantId, String userId, String sessionId, String question) {
        metrics.recordWorkflow();
        List<WorkflowState> trace = new ArrayList<>();
        trace.add(WorkflowState.PLANNING);
        SearchResponse response = null;
        for (int reflection = 0; reflection <= properties.getWorkflow().getMaxReflections(); reflection++) {
            trace.add(WorkflowState.EXECUTING);
            response = searchPipeline.search(tenantId, userId, question);
            trace.add(WorkflowState.REFLECTING);
            if (response.decision().sufficient()) {
                trace.add(WorkflowState.COMPLETED);
                checkpointPort.save(sessionId, trace);
                return new WorkflowResult(response, trace);
            }
            if (reflection == properties.getWorkflow().getMaxReflections()) {
                trace.add(WorkflowState.REFUSED);
                checkpointPort.save(sessionId, trace);
                return new WorkflowResult(response, trace);
            }
            trace.add(WorkflowState.REPLANNING);
        }
        throw new IllegalStateException("状态机未能在预期路径结束。");
    }
}
