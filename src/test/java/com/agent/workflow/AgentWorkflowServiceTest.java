package com.agent.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.config.AgentPlatformProperties;
import com.agent.metrics.PlatformMetrics;
import com.agent.retrieval.EvidenceDecision;
import com.agent.retrieval.SearchPipeline;
import com.agent.retrieval.SearchResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentWorkflowServiceTest {
    @Test
    void shouldRefuseAfterConfiguredReflectionLimit() {
        AgentPlatformProperties properties = new AgentPlatformProperties();
        properties.getWorkflow().setMaxReflections(1);
        SearchPipeline search = (tenant, user, question) -> new SearchResponse(question, List.of(), EvidenceDecision.refused("证据不足"));
        WorkflowCheckpointPort checkpoint = (session, trace) -> { };
        AgentWorkflowService service = new AgentWorkflowService(search, properties, checkpoint, new PlatformMetrics(new SimpleMeterRegistry()));
        WorkflowResult result = service.execute("tenant", "user", "session", "问题");
        assertThat(result.trace()).containsExactly(WorkflowState.PLANNING, WorkflowState.EXECUTING, WorkflowState.REFLECTING,
                WorkflowState.REPLANNING, WorkflowState.EXECUTING, WorkflowState.REFLECTING, WorkflowState.REFUSED);
    }
}
