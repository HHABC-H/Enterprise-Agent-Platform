/**
 * 本文件定义 {@code TraceIdHolder}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

public final class TraceIdHolder {
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private TraceIdHolder() { }
    public static void set(String traceId) { TRACE_ID.set(traceId); }
    public static String get() { return TRACE_ID.get(); }
    public static void clear() { TRACE_ID.remove(); }
}
