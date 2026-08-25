/**
 * 本文件定义 {@code GraphRelationSearchPort}，负责面向外部能力的端口与领域模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.extension;

import com.agent.document.DocumentIndex;
import java.util.List;

public interface GraphRelationSearchPort {
    default List<GraphRelation> search(GraphRelationQuery query) { return List.of(); }
    default void replaceDocument(String tenantId, String documentId, String version, DocumentIndex index) { }
    default void deleteDocument(String tenantId, String documentId) { }
    default boolean available() { return false; }
    default List<String> findRelatedChunks(String documentId, int maxHops) { return List.of(); }
}
