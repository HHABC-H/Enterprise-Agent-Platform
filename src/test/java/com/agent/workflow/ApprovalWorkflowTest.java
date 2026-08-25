package com.agent.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agent.config.AgentPlatformProperties;
import com.agent.infrastructure.workflow.InMemoryWorkflowCheckpointAdapter;
import com.agent.metrics.PlatformMetrics;
import com.agent.retrieval.EvidenceDecision;
import com.agent.retrieval.SearchPipeline;
import com.agent.retrieval.SearchResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApprovalWorkflowTest {
    @Test
    void 批准后可恢复且重复过期越权审批被拒绝() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC);
        InMemoryWorkflowCheckpointAdapter checkpoints = new InMemoryWorkflowCheckpointAdapter(clock);
        SearchPipeline search = (tenant, user, question) -> new SearchResponse(question, List.of(), EvidenceDecision.accepted());
        AgentWorkflowService service = new AgentWorkflowService(search, new AgentPlatformProperties(), checkpoints,
                new PlatformMetrics(new SimpleMeterRegistry()), clock);
        WorkflowResult waiting = service.execute("tenant-a", "owner", "session", "可审批问题", true);
        assertThat(waiting.waitingApproval()).isTrue();
        assertThat(waiting.trace()).contains(WorkflowState.WAITING_APPROVAL).doesNotContain(WorkflowState.COMPLETED);
        assertThatThrownBy(() -> service.resume(waiting.workflowId(), "not-reviewer", 0, ApprovalDecision.APPROVE, "通过"))
                .isInstanceOf(IllegalArgumentException.class);

        WorkflowResult completed = service.resume(waiting.workflowId(), "reviewer", 0, ApprovalDecision.APPROVE, "通过");
        assertThat(completed.trace()).contains(WorkflowState.COMPLETED);
        assertThatThrownBy(() -> service.resume(waiting.workflowId(), "reviewer", 1, ApprovalDecision.APPROVE, "重复"))
                .isInstanceOf(IllegalStateException.class);

        checkpoints.create(new WorkflowCheckpoint("expired", "tenant-a", "owner", "session", "摘要", "GENERATE_ANSWER", WorkflowState.WAITING_APPROVAL,
                Instant.parse("2026-08-23T00:00:00Z"), Instant.parse("2026-08-23T00:15:00Z"), "trace", 0, null, null, null, List.of(WorkflowState.WAITING_APPROVAL)));
        assertThatThrownBy(() -> service.resume("expired", "reviewer", 0, ApprovalDecision.APPROVE, "通过"))
                .isInstanceOf(IllegalStateException.class);
    }
}
