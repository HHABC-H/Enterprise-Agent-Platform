/**
 * 本文件定义 {@code WorkflowCheckpointPort}，负责工作流状态、人工审批与检查点管理。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.workflow;

import java.util.List;
import java.util.Optional;

public interface WorkflowCheckpointPort {
    void save(String sessionId, List<WorkflowState> states);
    default void create(WorkflowCheckpoint checkpoint) { throw new UnsupportedOperationException("当前检查点仓储不支持审批。"); }
    default Optional<WorkflowCheckpoint> find(String workflowId) { return Optional.empty(); }
    default WorkflowCheckpoint decide(String workflowId, long version, String approverId, ApprovalDecision decision, String comment) {
        throw new UnsupportedOperationException("当前检查点仓储不支持审批。");
    }
}
