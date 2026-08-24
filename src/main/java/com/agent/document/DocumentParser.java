package com.agent.document;

public interface DocumentParser {

    ParsedDocument parse(String documentId, String content, DocumentMetadata metadata);
}
