/**
 * 本文件定义 {@code WorkflowController}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import com.agent.workflow.AgentWorkflowService;
import com.agent.workflow.WorkflowCheckpoint;
import com.agent.workflow.WorkflowResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    private final AgentWorkflowService workflows;
    public WorkflowController(AgentWorkflowService workflows) { this.workflows = workflows; }
    @GetMapping("/{workflowId}") public ApiResponse<WorkflowCheckpoint> get(@PathVariable String workflowId) { return ApiResponse.of(workflows.get(workflowId)); }
    @PostMapping("/{workflowId}/approval")
    public ApiResponse<ChatResponse> approve(@PathVariable String workflowId, @Valid @RequestBody ApprovalRequest request) {
        WorkflowResult result = workflows.resume(workflowId, request.approverId(), request.version(), request.decision(), request.comment());
        return ApiResponse.of(new ChatResponse(null, result.searchResponse().decision().sufficient() ? false : true,
                result.searchResponse().decision().refusalReason(), result.searchResponse().evidence().stream().map(EvidenceResponse::from).toList(), result.trace(), result.workflowId(), false));
    }
}
