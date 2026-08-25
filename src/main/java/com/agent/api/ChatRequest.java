/**
 * 本文件定义 {@code ChatRequest}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "tenantId 不能为空") String tenantId,
        @NotBlank(message = "userId 不能为空") String userId,
        @NotBlank(message = "sessionId 不能为空") String sessionId,
        @NotBlank(message = "question 不能为空") String question,
        Boolean requireApproval) {
    public boolean approvalRequired() { return Boolean.TRUE.equals(requireApproval); }
}
