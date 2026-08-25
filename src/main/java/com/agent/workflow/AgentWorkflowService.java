/**
 * 本文件定义 {@code AgentWorkflowService}，负责工作流状态、人工审批与检查点管理。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.workflow;

import com.agent.config.AgentPlatformProperties;
import com.agent.metrics.PlatformMetrics;
import com.agent.retrieval.SearchPipeline;
import com.agent.retrieval.SearchResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentWorkflowService {
    private final SearchPipeline searchPipeline;
    private final AgentPlatformProperties properties;
    private final WorkflowCheckpointPort checkpointPort;
    private final PlatformMetrics metrics;
    private final Clock clock;
    @Autowired
    public AgentWorkflowService(SearchPipeline searchPipeline, AgentPlatformProperties properties, WorkflowCheckpointPort checkpointPort,
                                PlatformMetrics metrics, Clock clock) {
        this.searchPipeline = searchPipeline; this.properties = properties; this.checkpointPort = checkpointPort; this.metrics = metrics; this.clock = clock;
    }
    /** 保留原测试和调用方可用的构造方式。 */
    public AgentWorkflowService(SearchPipeline searchPipeline, AgentPlatformProperties properties, WorkflowCheckpointPort checkpointPort, PlatformMetrics metrics) {
        this(searchPipeline, properties, checkpointPort, metrics, Clock.systemUTC());
    }

    /** 默认直接完成检索与回答，不进入人工审批阶段。 */
    public WorkflowResult execute(String tenantId, String userId, String sessionId, String question) {
        return execute(tenantId, userId, sessionId, question, false);
    }

    /**
     * 驱动规划、检索和反思状态机；证据充分时按请求创建审批检查点或直接完成。
     */
    public WorkflowResult execute(String tenantId, String userId, String sessionId, String question, boolean requireApproval) {
        metrics.recordWorkflow();
        List<WorkflowState> trace = new ArrayList<>(); trace.add(WorkflowState.PLANNING);
        SearchResponse response = null;
        for (int reflection = 0; reflection <= properties.getWorkflow().getMaxReflections(); reflection++) {
            trace.add(WorkflowState.EXECUTING); response = searchPipeline.search(tenantId, userId, question); trace.add(WorkflowState.REFLECTING);
            if (response.decision().sufficient()) {
                if (requireApproval) {
                    String workflowId = UUID.randomUUID().toString(); trace.add(WorkflowState.WAITING_APPROVAL);
                    Instant now = Instant.now(clock);
                    checkpointPort.create(new WorkflowCheckpoint(workflowId, tenantId, userId, sessionId, summarize(question), "GENERATE_ANSWER",
                            WorkflowState.WAITING_APPROVAL, now, now.plus(Duration.ofMinutes(15)), null, 0, null, null, null, trace));
                    metrics.recordApprovalWaiting();
                    return new WorkflowResult(response, trace, workflowId, true);
                }
                trace.add(WorkflowState.COMPLETED); checkpointPort.save(sessionId, trace); return new WorkflowResult(response, trace);
            }
            if (reflection == properties.getWorkflow().getMaxReflections()) {
                trace.add(WorkflowState.REFUSED); checkpointPort.save(sessionId, trace); return new WorkflowResult(response, trace);
            }
            trace.add(WorkflowState.REPLANNING);
        }
        throw new IllegalStateException("状态机未能在预期路径结束。");
    }

    /** 校验审批人、乐观锁版本和意见后恢复工作流；拒绝时返回拒绝结果而不再次执行检索。 */
    public WorkflowResult resume(String workflowId, String approverId, long version, ApprovalDecision decision, String comment) {
        if (!approvers().contains(approverId)) { throw new IllegalArgumentException("当前用户没有审批权限。"); }
        if (comment == null || comment.isBlank() || comment.length() > 500) { throw new IllegalArgumentException("审批意见不能为空且不能超过 500 个字符。"); }
        WorkflowCheckpoint checkpoint = checkpointPort.decide(workflowId, version, approverId, decision, comment.trim());
        if (decision == ApprovalDecision.REJECT) {
            metrics.recordApprovalRejected();
            List<WorkflowState> trace = new ArrayList<>(checkpoint.trace()); trace.add(WorkflowState.REFUSED);
            return new WorkflowResult(new SearchResponse(checkpoint.inputSummary(), List.of(), com.agent.retrieval.EvidenceDecision.refused("审批已拒绝。")), trace, workflowId, false);
        }
        metrics.recordApprovalApproved();
        return execute(checkpoint.tenantId(), checkpoint.ownerUserId(), checkpoint.sessionId(), checkpoint.inputSummary(), false);
    }
    public WorkflowCheckpoint get(String workflowId) { return checkpointPort.find(workflowId).orElseThrow(() -> new IllegalArgumentException("未找到审批工作流。")); }
    private String summarize(String question) { return question.length() > 200 ? question.substring(0, 200) : question; }
    private java.util.Set<String> approvers() {
        String value = System.getenv().getOrDefault("AI_PLATFORM_WORKFLOW_APPROVER_IDS", "reviewer");
        return java.util.Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).collect(java.util.stream.Collectors.toSet());
    }
}
