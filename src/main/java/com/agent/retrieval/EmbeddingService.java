package com.agent.retrieval;

public interface EmbeddingService {

    double similarity(String left, String right);
}
