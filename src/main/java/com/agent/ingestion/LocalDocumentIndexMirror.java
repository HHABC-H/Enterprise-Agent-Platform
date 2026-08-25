/**
 * 本文件定义 {@code LocalDocumentIndexMirror}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

import com.agent.document.Chunk;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** local profile 中明确共享的当前检索索引镜像。 */
@Component
public class LocalDocumentIndexMirror {
    private static final CopyOnWriteArrayList<Chunk> chunks = new CopyOnWriteArrayList<>();
    public void replace(String documentId, List<Chunk> values) {
        chunks.removeIf(item -> item.documentId().equals(documentId)); chunks.addAll(values);
    }
    public List<Chunk> findAll() { return List.copyOf(chunks); }
}
