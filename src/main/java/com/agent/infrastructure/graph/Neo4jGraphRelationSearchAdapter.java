/**
 * 本文件定义 {@code Neo4jGraphRelationSearchAdapter}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.graph;

import com.agent.document.Chunk;
import com.agent.document.DocumentIndex;
import com.agent.document.ParentChunk;
import com.agent.extension.GraphRelation;
import com.agent.extension.GraphRelationQuery;
import com.agent.extension.GraphRelationSearchPort;
import com.agent.metrics.PlatformMetrics;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionConfig;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Neo4j 图谱适配器：查询模板固定，租户、文档、版本与访问条件均在 Cypher 中约束。 */
@Component
@Profile({"docker", "local-docker"})
public class Neo4jGraphRelationSearchAdapter implements GraphRelationSearchPort {
    private static final Pattern ENTITY = Pattern.compile("`([^`]{1,80})`|\\b[A-Z][A-Za-z0-9_]{2,}\\b");
    private static final TransactionConfig QUERY_TIMEOUT = TransactionConfig.builder().withTimeout(Duration.ofSeconds(2)).build();
    private final Driver driver;
    private final PlatformMetrics metrics;
    public Neo4jGraphRelationSearchAdapter(Driver driver, PlatformMetrics metrics) { this.driver = driver; this.metrics = metrics; }

    /** 先清除文档的旧版本节点，再在同一写事务中写入文档、父块、子块和实体关系。 */
    @Override
    public void replaceDocument(String tenantId, String documentId, String version, DocumentIndex index) {
        DocumentMetadataView metadata = metadata(index);
        try (Session session = driver.session()) {
            session.executeWrite(transaction -> {
                Map<String, Object> document = base(tenantId, documentId, version);
                document.put("id", documentKey(tenantId, documentId, version));
                document.put("permissionTags", metadata.permissionTags());
                document.put("allowedUserIds", metadata.allowedUserIds());
                transaction.run("MATCH (node {tenantId: $tenantId, documentId: $documentId}) DETACH DELETE node", Map.of("tenantId", tenantId, "documentId", documentId)).consume();
                transaction.run("CREATE (d:Document $document)", Map.of("document", document)).consume();
                for (ParentChunk parent : index.parents()) { writeParent(transaction, tenantId, documentId, version, parent); }
                for (Chunk chunk : index.chunks()) { writeChunk(transaction, tenantId, documentId, version, chunk); }
                return null;
            });
        }
    }

    /** 按租户和文档删除全量图数据，避免遗留关系指向已删除节点。 */
    @Override
    public void deleteDocument(String tenantId, String documentId) {
        try (Session session = driver.session()) {
            session.executeWrite(transaction -> transaction.run(
                    "MATCH (node {tenantId: $tenantId, documentId: $documentId}) DETACH DELETE node",
                    Map.of("tenantId", tenantId, "documentId", documentId)).consume());
        }
    }

    /** 在图谱指标计时范围内执行受约束的关系查询。 */
    @Override
    public List<GraphRelation> search(GraphRelationQuery query) {
        return metrics.recordGraph(() -> searchRestricted(query));
    }

    /** Cypher 固定查询范围、权限条件和返回上限，调用方不能拼接任意图查询。 */
    private List<GraphRelation> searchRestricted(GraphRelationQuery query) {
        String cypher = "MATCH path=(d:Document {tenantId: $tenantId, documentId: $documentId, version: $version})-[rels*1.."
                + query.maxHops() + "]->(target:Chunk) "
                + "WHERE all(node IN nodes(path) WHERE node.tenantId = $tenantId AND node.documentId = $documentId AND node.version = $version) "
                + "AND (size(target.permissionTags) = 0 OR 'public' IN target.permissionTags OR ('user:' + $userId) IN target.permissionTags OR $userId IN target.allowedUserIds) "
                + "UNWIND relationships(path) AS rel RETURN DISTINCT startNode(rel).id AS fromId, endNode(rel).id AS toId, type(rel) AS type, length(path) AS hops LIMIT $limit";
        Map<String, Object> parameters = Map.of("tenantId", query.tenantId(), "documentId", query.documentId(), "version", query.version(),
                "userId", query.userId(), "limit", query.limit());
        try (Session session = driver.session()) {
            return session.executeRead(transaction -> transaction.run(cypher, parameters).list(record -> new GraphRelation(
                    record.get("fromId").asString(), record.get("toId").asString(), record.get("type").asString(), record.get("hops").asInt())), QUERY_TIMEOUT);
        }
    }

    @Override public boolean available() { return true; }

    /** 写入父上下文节点，并通过 CONTAINS 关系关联到文档节点。 */
    private void writeParent(org.neo4j.driver.TransactionContext transaction, String tenantId, String documentId, String version, ParentChunk parent) {
        Map<String, Object> values = base(tenantId, documentId, version);
        values.put("id", parent.parentChunkId());
        values.put("content", parent.content());
        values.put("permissionTags", parent.metadata().permissionTags());
        values.put("allowedUserIds", parent.metadata().allowedUserIds());
        transaction.run("MATCH (d:Document {id: $documentKey, tenantId: $tenantId, documentId: $documentId, version: $version}) "
                        + "CREATE (p:ParentChunk $parent) CREATE (d)-[:CONTAINS $relation]->(p)",
                Map.of("documentKey", documentKey(tenantId, documentId, version), "tenantId", tenantId, "documentId", documentId,
                        "version", version, "parent", values, "relation", base(tenantId, documentId, version))).consume();
    }

    /** 写入子块及其可抽取实体；所有节点均携带租户、文档和版本边界。 */
    private void writeChunk(org.neo4j.driver.TransactionContext transaction, String tenantId, String documentId, String version, Chunk chunk) {
        Map<String, Object> values = base(tenantId, documentId, version);
        values.put("id", chunk.chunkId());
        values.put("content", chunk.content());
        values.put("permissionTags", chunk.metadata().permissionTags());
        values.put("allowedUserIds", chunk.metadata().allowedUserIds());
        transaction.run("MATCH (p:ParentChunk {id: $parentId, tenantId: $tenantId, documentId: $documentId, version: $version}) "
                        + "CREATE (c:Chunk $chunk) CREATE (p)-[:CONTAINS $relation]->(c)",
                Map.of("parentId", chunk.parentChunkId(), "tenantId", tenantId, "documentId", documentId, "version", version,
                        "chunk", values, "relation", base(tenantId, documentId, version))).consume();
        for (String entity : entities(chunk)) {
            Map<String, Object> entityValues = base(tenantId, documentId, version);
            entityValues.put("id", "entity:" + chunk.chunkId() + ":" + entity);
            entityValues.put("name", entity);
            transaction.run("MATCH (c:Chunk {id: $chunkId, tenantId: $tenantId, documentId: $documentId, version: $version}) "
                            + "MERGE (e:Entity {id: $entity.id, tenantId: $tenantId, documentId: $documentId, version: $version}) "
                            + "SET e += $entity CREATE (c)-[:RELATES_TO $relation]->(e)",
                    Map.of("chunkId", chunk.chunkId(), "tenantId", tenantId, "documentId", documentId, "version", version,
                            "entity", entityValues, "relation", base(tenantId, documentId, version))).consume();
        }
    }

    private List<String> entities(Chunk chunk) {
        List<String> values = new ArrayList<>(chunk.headingPath());
        Matcher matcher = ENTITY.matcher(chunk.content());
        while (matcher.find()) { values.add(matcher.group(1) == null ? matcher.group() : matcher.group(1)); }
        return values.stream().filter(value -> !value.isBlank()).distinct().toList();
    }

    private Map<String, Object> base(String tenantId, String documentId, String version) {
        Map<String, Object> values = new HashMap<>();
        values.put("tenantId", tenantId); values.put("documentId", documentId); values.put("version", version);
        return values;
    }
    private String documentKey(String tenantId, String documentId, String version) { return tenantId + ":" + documentId + ":" + version; }
    private DocumentMetadataView metadata(DocumentIndex index) {
        if (!index.chunks().isEmpty()) {
            Chunk first = index.chunks().get(0);
            return new DocumentMetadataView(first.metadata().permissionTags(), first.metadata().allowedUserIds());
        }
        return new DocumentMetadataView(java.util.Set.of(), java.util.Set.of());
    }
    private record DocumentMetadataView(java.util.Set<String> permissionTags, java.util.Set<String> allowedUserIds) { }
}
