package com.agent.infrastructure.retrieval;

import com.agent.retrieval.EmbeddingService;
import com.agent.retrieval.TextVectorSupport;
import org.springframework.stereotype.Component;

@Component
public class HashEmbeddingService implements EmbeddingService {
    @Override
    public double similarity(String left, String right) {
        return TextVectorSupport.cosine(left, right);
    }
}
