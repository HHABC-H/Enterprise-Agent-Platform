/**
 * 本文件定义 {@code EvidenceResponse}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import com.agent.retrieval.RetrievalEvidence;
import java.util.Set;

public record EvidenceResponse(String documentId, String chunkId, String source, double score, Set<String> channels) {
    public static EvidenceResponse from(RetrievalEvidence evidence) {
        return new EvidenceResponse(evidence.chunk().documentId(), evidence.chunk().chunkId(), evidence.chunk().metadata().source(),
                evidence.score(), evidence.sources().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
}
