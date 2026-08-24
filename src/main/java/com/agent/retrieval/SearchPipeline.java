package com.agent.retrieval;

public interface SearchPipeline {
    SearchResponse search(String tenantId, String userId, String question);
}
