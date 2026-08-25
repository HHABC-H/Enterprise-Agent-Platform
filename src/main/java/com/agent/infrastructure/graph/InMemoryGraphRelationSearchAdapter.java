/**
 * 本文件定义 {@code InMemoryGraphRelationSearchAdapter}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.graph;

import com.agent.document.Chunk;
import com.agent.document.DocumentIndex;
import com.agent.extension.GraphRelation;
import com.agent.extension.GraphRelationQuery;
import com.agent.extension.GraphRelationSearchPort;
import com.agent.retrieval.SecurityFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local profile 的可测试图谱降级实现。 */
@Component
@Profile("local")
public class InMemoryGraphRelationSearchAdapter implements GraphRelationSearchPort {
    private final ConcurrentHashMap<String, List<GraphRelation>> relations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Chunk>> chunks = new ConcurrentHashMap<>();
    private final SecurityFilter securityFilter;
    public InMemoryGraphRelationSearchAdapter(SecurityFilter securityFilter) { this.securityFilter = securityFilter; }
    @Override public void replaceDocument(String tenantId, String documentId, String version, DocumentIndex index) {
        String key = key(tenantId, documentId, version);
        List<GraphRelation> values = new ArrayList<>();
        for (var parent : index.parents()) {
            values.add(new GraphRelation(documentId, parent.parentChunkId(), "CONTAINS", 1));
        }
        for (Chunk child : index.chunks()) {
            values.add(new GraphRelation(child.parentChunkId(), child.chunkId(), "CONTAINS", 1));
            for (String entity : entities(child)) { values.add(new GraphRelation(child.chunkId(), entity, "RELATES_TO", 1)); }
        }
        relations.put(key, List.copyOf(values)); chunks.put(key, index.chunks());
    }
    @Override public List<GraphRelation> search(GraphRelationQuery query) {
        String key = key(query.tenantId(), query.documentId(), query.version());
        List<Chunk> visible = securityFilter.filter(query.tenantId(), query.userId(), chunks.getOrDefault(key, List.of()));
        java.util.Set<String> visibleChunkIds = visible.stream().map(Chunk::chunkId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> visibleParentIds = visible.stream().map(Chunk::parentChunkId).collect(java.util.stream.Collectors.toSet());
        return relations.getOrDefault(key, List.of()).stream()
                .filter(item -> item.hops() <= query.maxHops())
                .filter(item -> isVisible(item, query.documentId(), visibleChunkIds, visibleParentIds))
                .limit(query.limit()).toList();
    }
    @Override public void deleteDocument(String tenantId, String documentId) {
        relations.keySet().removeIf(value -> value.startsWith(tenantId + ":" + documentId + ":"));
        chunks.keySet().removeIf(value -> value.startsWith(tenantId + ":" + documentId + ":"));
    }
    @Override public boolean available() { return true; }
    private List<String> entities(Chunk chunk) {
        List<String> values = new ArrayList<>(chunk.headingPath());
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("`([^`]{1,80})`|\\b[A-Z][A-Za-z0-9_]{2,}\\b").matcher(chunk.content());
        while (matcher.find()) { values.add(matcher.group(1) == null ? matcher.group() : matcher.group(1)); }
        return values.stream().filter(value -> !value.isBlank()).distinct().toList();
    }
    private boolean isVisible(GraphRelation relation, String documentId, java.util.Set<String> visibleChunkIds, java.util.Set<String> visibleParentIds) {
        if (relation.fromId().equals(documentId)) { return visibleParentIds.contains(relation.toId()); }
        if (relation.type().equals("RELATES_TO")) { return visibleChunkIds.contains(relation.fromId()); }
        return visibleParentIds.contains(relation.fromId()) && visibleChunkIds.contains(relation.toId());
    }
    private String key(String tenantId, String documentId, String version) { return tenantId + ":" + documentId + ":" + version; }
}
