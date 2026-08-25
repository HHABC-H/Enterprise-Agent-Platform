/**
 * 本文件定义 {@code ApiResponse}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

public record ApiResponse<T>(String traceId, T data) {
    public static <T> ApiResponse<T> of(T data) { return new ApiResponse<>(TraceIdHolder.get(), data); }
}
