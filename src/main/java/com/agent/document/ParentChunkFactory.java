/**
 * 本文件定义 {@code ParentChunkFactory}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 将同一标题路径下的子块聚合为可审计的父上下文。 */
@Component
public class ParentChunkFactory {
    public DocumentIndex create(String documentId, List<Chunk> children) {
        Map<List<String>, List<Chunk>> groups = new LinkedHashMap<>();
        for (Chunk child : children) {
            groups.computeIfAbsent(child.headingPath(), ignored -> new ArrayList<>()).add(child);
        }
        List<ParentChunk> parents = new ArrayList<>();
        List<Chunk> indexedChildren = new ArrayList<>();
        int ordinal = 0;
        for (List<Chunk> group : groups.values()) {
            Chunk first = group.get(0);
            String content = group.stream().map(Chunk::content).reduce("", (left, right) -> left.isEmpty() ? right : left + "\n\n" + right);
            String parentId = sha256(documentId + ":parent:" + ordinal++ + ":" + String.join("/", first.headingPath()));
            parents.add(new ParentChunk(parentId, documentId, content, first.headingPath(), first.metadata()));
            for (Chunk child : group) {
                indexedChildren.add(new Chunk(child.chunkId(), child.documentId(), child.content(), child.headingPath(), child.metadata(), parentId));
            }
        }
        return new DocumentIndex(documentId, parents, indexedChildren);
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : bytes) { result.append(String.format("%02x", item)); }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256。", exception);
        }
    }
}
