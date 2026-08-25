/**
 * 本文件定义 {@code ApprovalRequest}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import com.agent.workflow.ApprovalDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApprovalRequest(@NotBlank(message = "审批人不能为空") String approverId,
                              @NotNull(message = "审批决定不能为空") ApprovalDecision decision,
                              @NotBlank(message = "审批意见不能为空") String comment,
                              long version) { }
