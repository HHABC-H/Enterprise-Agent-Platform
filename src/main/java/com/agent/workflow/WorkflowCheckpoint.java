/**
 * 本文件定义 {@code WorkflowCheckpoint}，负责工作流状态、人工审批与检查点管理。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.workflow;

import java.time.Instant;
import java.util.List;

/** 不含完整对话正文的人工审批检查点。 */
public record WorkflowCheckpoint(String workflowId, String tenantId, String ownerUserId, String sessionId,
                                 String inputSummary, String pendingAction, WorkflowState state,
                                 Instant createdAt, Instant expiresAt, String traceId, long version,
                                 String approverId, ApprovalDecision decision, String approvalComment,
                                 List<WorkflowState> trace) {
    public WorkflowCheckpoint { trace = List.copyOf(trace); }
}
