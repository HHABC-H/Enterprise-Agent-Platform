/**
 * 本文件定义 {@code WorkflowState}，负责工作流状态、人工审批与检查点管理。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.workflow;

public enum WorkflowState {
    PLANNING, EXECUTING, REFLECTING, REPLANNING, WAITING_APPROVAL, COMPLETED, REFUSED, FAILED
}
