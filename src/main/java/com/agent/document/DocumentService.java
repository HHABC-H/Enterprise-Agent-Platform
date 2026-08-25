/**
 * 本文件定义 {@code DocumentService}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.document;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentParser parser;
    private final DocumentSplitter splitter;
    private final ParentChunkFactory parentChunkFactory;
    private final DocumentChunkStore store;

    public DocumentService(DocumentParser parser, DocumentSplitter splitter, ParentChunkFactory parentChunkFactory, DocumentChunkStore store) {
        this.parser = parser;
        this.splitter = splitter;
        this.parentChunkFactory = parentChunkFactory;
        this.store = store;
    }

    /** 保留同步调用契约：构建索引并返回子块列表。 */
    public List<Chunk> ingest(String documentId, String markdown, DocumentMetadata metadata) {
        return rebuild(documentId, markdown, metadata).chunks();
    }

    /** 原子替换本地父子索引，避免旧版本的块与新版本混用。 */
    public DocumentIndex rebuild(String documentId, String markdown, DocumentMetadata metadata) {
        DocumentIndex index = build(documentId, markdown, metadata);
        store.delete(documentId);
        store.save(documentId, index.parents(), index.chunks());
        return index;
    }

    /** 仅构建索引对象，不写入仓储，供异步入库流程复用。 */
    public DocumentIndex build(String documentId, String markdown, DocumentMetadata metadata) {
        ParsedDocument parsedDocument = parser.parse(documentId, markdown, metadata);
        List<Chunk> chunks = splitter.split(parsedDocument);
        return parentChunkFactory.create(documentId, chunks);
    }

    /** 删除文档的本地父子索引，外部存储由调用方按相同幂等键清理。 */
    public void delete(String documentId) {
        store.delete(documentId);
    }
}
