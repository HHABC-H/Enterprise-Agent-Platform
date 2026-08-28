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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    private final AgentWorkflowService workflows;
    private final IdentityGuard identityGuard;
    public WorkflowController(AgentWorkflowService workflows, IdentityGuard identityGuard) { this.workflows = workflows; this.identityGuard = identityGuard; }
    @GetMapping("/{workflowId}") public ApiResponse<WorkflowCheckpoint> get(@PathVariable String workflowId) {
        WorkflowCheckpoint checkpoint = workflows.get(workflowId); identityGuard.assertTenant(checkpoint.tenantId()); return ApiResponse.of(checkpoint);
    }
    @GetMapping("/pending")
    public ApiResponse<java.util.List<WorkflowCheckpoint>> pending(@RequestParam String tenantId, @RequestParam String userId,
                                                                      @RequestParam(defaultValue = "false") boolean approver) {
        identityGuard.assertRequestIdentity(tenantId, userId);
        return ApiResponse.of(workflows.pending(tenantId, userId, identityGuard.isApprover()));
    }
    @PostMapping("/{workflowId}/approval")
    public ApiResponse<ChatResponse> approve(@PathVariable String workflowId, @Valid @RequestBody ApprovalRequest request) {
        WorkflowResult result = workflows.resume(workflowId, identityGuard.approverId(request.approverId()), request.version(), request.decision(), request.comment());
        return ApiResponse.of(new ChatResponse(null, !result.searchResponse().decision().sufficient(),
                result.searchResponse().decision().refusalReason(), result.searchResponse().evidence().stream().map(EvidenceResponse::from).toList(), result.trace(), result.workflowId(), false, false, java.util.List.of()));
    }
}
