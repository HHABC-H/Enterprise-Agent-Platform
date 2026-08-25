/**
 * 本文件定义 {@code SearchCandidate}，负责检索、权限过滤、证据校验与排序流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.retrieval;

import com.agent.document.Chunk;

public record SearchCandidate(Chunk chunk, double score, SearchSource source) {
}
