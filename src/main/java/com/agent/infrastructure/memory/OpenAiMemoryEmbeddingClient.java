package com.agent.infrastructure.memory;

import com.agent.config.AgentPlatformProperties;
import com.agent.memory.MemoryEmbeddingClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "ai-platform.memory.long-term.embedding", name = "enabled", havingValue = "true")
public class OpenAiMemoryEmbeddingClient implements MemoryEmbeddingClient {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final AgentPlatformProperties properties;
    public OpenAiMemoryEmbeddingClient(AgentPlatformProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.client = RestClient.builder().baseUrl(properties.getMemory().getLongTerm().getEmbedding().getBaseUrl()).build();
    }
    @Override public Optional<float[]> embed(String text) {
        String apiKey = properties.getMemory().getLongTerm().getEmbedding().getApiKey();
        if (apiKey == null || apiKey.isBlank()) return Optional.empty();
        String response = client.post().uri("/embeddings").header("Authorization", "Bearer " + apiKey)
                .body(Map.of("model", properties.getMemory().getLongTerm().getEmbedding().getModelName(), "input", text))
                .retrieve().body(String.class);
        try {
            JsonNode items = mapper.readTree(response).path("data");
            JsonNode values = items.path(0).path("embedding");
            if (!values.isArray() || values.size() != properties.getMemory().getLongTerm().getEmbedding().getDimensions()) return Optional.empty();
            float[] vector = new float[values.size()];
            for (int index = 0; index < values.size(); index++) vector[index] = (float) values.get(index).asDouble();
            return Optional.of(vector);
        } catch (Exception exception) { throw new IllegalStateException("向量响应解析失败。", exception); }
    }
}
