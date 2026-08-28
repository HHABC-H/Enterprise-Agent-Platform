package com.agent.memory;

import java.util.Optional;

public interface MemoryEmbeddingClient {
    Optional<float[]> embed(String text);
}
