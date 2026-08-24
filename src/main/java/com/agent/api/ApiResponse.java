package com.agent.api;

public record ApiResponse<T>(String traceId, T data) {
    public static <T> ApiResponse<T> of(T data) { return new ApiResponse<>(TraceIdHolder.get(), data); }
}
