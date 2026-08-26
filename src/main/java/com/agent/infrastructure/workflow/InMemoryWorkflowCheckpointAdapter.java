/**
 * 本文件定义 {@code InMemoryWorkflowCheckpointAdapter}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.workflow;

import com.agent.workflow.ApprovalDecision;
import com.agent.workflow.WorkflowCheckpoint;
import com.agent.workflow.WorkflowCheckpointPort;
import com.agent.workflow.WorkflowState;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("local")
public class InMemoryWorkflowCheckpointAdapter implements WorkflowCheckpointPort {
    private final ConcurrentHashMap<String, List<WorkflowState>> traces = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WorkflowCheckpoint> checkpoints = new ConcurrentHashMap<>();
    private final Clock clock;
    public InMemoryWorkflowCheckpointAdapter(Clock clock) { this.clock = clock; }
    @Override public void save(String sessionId, List<WorkflowState> states) { traces.put(sessionId, List.copyOf(states)); }
    @Override public void create(WorkflowCheckpoint checkpoint) { checkpoints.putIfAbsent(checkpoint.workflowId(), checkpoint); }
    @Override public Optional<WorkflowCheckpoint> find(String workflowId) { return Optional.ofNullable(checkpoints.get(workflowId)); }
    @Override public List<WorkflowCheckpoint> findPending(String tenantId, String userId, boolean approver) {
        return checkpoints.values().stream().filter(item -> item.tenantId().equals(tenantId))
                .filter(item -> approver || item.ownerUserId().equals(userId))
                .filter(item -> item.state() == WorkflowState.WAITING_APPROVAL).toList();
    }
    @Override public WorkflowCheckpoint decide(String workflowId, long version, String approverId, ApprovalDecision decision, String comment) {
        return checkpoints.compute(workflowId, (ignored, current) -> {
            if (current == null) { throw new IllegalArgumentException("未找到审批工作流。"); }
            if (current.state() != WorkflowState.WAITING_APPROVAL) { throw new IllegalStateException("当前工作流不在等待审批状态。"); }
            if (current.expiresAt().isBefore(Instant.now(clock))) { throw new IllegalStateException("审批检查点已过期。"); }
            if (current.version() != version) { throw new IllegalStateException("审批版本已变化，请刷新后重试。"); }
            return new WorkflowCheckpoint(current.workflowId(), current.tenantId(), current.ownerUserId(), current.sessionId(), current.inputSummary(),
                    current.pendingAction(), decision == ApprovalDecision.APPROVE ? WorkflowState.EXECUTING : WorkflowState.REFUSED,
                    current.createdAt(), current.expiresAt(), current.traceId(), current.version() + 1, approverId, decision, comment, current.trace());
        });
    }
}
