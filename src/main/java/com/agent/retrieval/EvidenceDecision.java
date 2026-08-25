/**
 * 本文件定义 {@code EvidenceDecision}，负责检索、权限过滤、证据校验与排序流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.retrieval;

public record EvidenceDecision(boolean sufficient, String refusalReason) {

    public static EvidenceDecision accepted() {
        return new EvidenceDecision(true, null);
    }

    public static EvidenceDecision refused(String reason) {
        return new EvidenceDecision(false, reason);
    }
}
