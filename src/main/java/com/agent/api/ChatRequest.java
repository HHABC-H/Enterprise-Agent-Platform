package com.agent.api;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "tenantId 不能为空") String tenantId,
        @NotBlank(message = "userId 不能为空") String userId,
        @NotBlank(message = "sessionId 不能为空") String sessionId,
        @NotBlank(message = "question 不能为空") String question) {
}
