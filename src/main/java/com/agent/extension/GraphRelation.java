/**
 * 本文件定义 {@code GraphRelation}，负责面向外部能力的端口与领域模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.extension;

/** 已过滤的图关系，不含文档正文。 */
public record GraphRelation(String fromId, String toId, String type, int hops) { }
