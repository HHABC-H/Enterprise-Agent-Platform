package com.agent.document;

import java.util.List;

/** 文档标题节的父上下文，子块只负责检索。 */
public record ParentChunk(String parentChunkId, String documentId, String content, List<String> headingPath,
                          DocumentMetadata metadata) {
    public ParentChunk {
        headingPath = headingPath == null ? List.of() : List.copyOf(headingPath);
    }
}
