package com.agent.document;

public record ParsedDocument(String documentId, String markdown, DocumentMetadata metadata) {
}
