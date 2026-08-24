package com.agent.extension;

public interface KnowledgeIngestionEventPublisher {
    void publish(String documentId, String contentSha256, String version, String idempotencyKey);
}
