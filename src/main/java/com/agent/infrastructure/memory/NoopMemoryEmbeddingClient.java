package com.agent.infrastructure.memory;

import com.agent.memory.MemoryEmbeddingClient;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai-platform.memory.long-term.embedding", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopMemoryEmbeddingClient implements MemoryEmbeddingClient {
    @Override public Optional<float[]> embed(String text) { return Optional.empty(); }
}
