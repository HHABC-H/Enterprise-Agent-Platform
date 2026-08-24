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

    public List<Chunk> ingest(String documentId, String markdown, DocumentMetadata metadata) {
        return build(documentId, markdown, metadata).chunks();
    }

    public DocumentIndex rebuild(String documentId, String markdown, DocumentMetadata metadata) {
        DocumentIndex index = build(documentId, markdown, metadata);
        store.delete(documentId);
        store.save(documentId, index.parents(), index.chunks());
        return index;
    }

    public DocumentIndex build(String documentId, String markdown, DocumentMetadata metadata) {
        ParsedDocument parsedDocument = parser.parse(documentId, markdown, metadata);
        List<Chunk> chunks = splitter.split(parsedDocument);
        return parentChunkFactory.create(documentId, chunks);
    }
}
