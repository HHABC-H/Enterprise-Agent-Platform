/**
 * 本文件定义 {@code GraphRelationQuery}，负责面向外部能力的端口与领域模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.extension;

/** 已校验的受控图查询参数。 */
public record GraphRelationQuery(String tenantId, String userId, String documentId, String version, int maxHops, int limit) {
    public GraphRelationQuery {
        if (maxHops < 1 || maxHops > 3) { throw new IllegalArgumentException("图谱最大跳数必须在 1 到 3 之间。"); }
        if (limit < 1 || limit > 50) { throw new IllegalArgumentException("图谱返回数量必须在 1 到 50 之间。"); }
    }
}
