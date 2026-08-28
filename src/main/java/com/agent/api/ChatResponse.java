/**
 * 本文件定义 {@code ChatResponse}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import com.agent.chat.ChatResult;
import com.agent.workflow.WorkflowState;
import java.util.List;

public record ChatResponse(String answer, boolean refused, String refusalReason, List<EvidenceResponse> evidence,
                           List<WorkflowState> trace, String workflowId, boolean waitingApproval,
                           boolean graphEvidenceUsed, List<com.agent.chat.WebSearchResult> webResults) {
    public static ChatResponse from(ChatResult result) {
        return new ChatResponse(result.answer(), result.refused(), result.refusalReason(), result.evidence().stream().map(EvidenceResponse::from).toList(),
                result.trace(), result.workflowId(), result.waitingApproval(), false, result.webResults());
    }
}
