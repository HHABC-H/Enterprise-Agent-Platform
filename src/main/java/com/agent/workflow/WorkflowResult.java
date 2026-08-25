/**
 * 本文件定义 {@code WorkflowResult}，负责工作流状态、人工审批与检查点管理。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.workflow;

import com.agent.retrieval.SearchResponse;
import java.util.List;

public record WorkflowResult(SearchResponse searchResponse, List<WorkflowState> trace, String workflowId, boolean waitingApproval) {
    public WorkflowResult { trace = List.copyOf(trace); }
    public WorkflowResult(SearchResponse searchResponse, List<WorkflowState> trace) { this(searchResponse, trace, null, false); }
}
