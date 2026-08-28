package com.agent.infrastructure.chat;

import com.agent.chat.WebSearchResult;
import com.agent.chat.WebSearchTool;
import com.agent.config.AgentPlatformProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "ai-platform.web-search", name = "enabled", havingValue = "true")
public class FirecrawlWebSearchTool implements WebSearchTool {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final AgentPlatformProperties properties;
    public FirecrawlWebSearchTool(AgentPlatformProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.client = RestClient.builder().baseUrl(properties.getWebSearch().getBaseUrl()).build();
    }
    @Override public List<WebSearchResult> search(String question) {
        String apiKey = properties.getWebSearch().getApiKey();
        if (apiKey == null || apiKey.isBlank()) return List.of();
        String response = client.post().uri("/v1/search").header("Authorization", "Bearer " + apiKey)
                .body(Map.of("query", question, "limit", properties.getWebSearch().getLimit())).retrieve().body(String.class);
        try {
            JsonNode data = mapper.readTree(response).path("data");
            List<WebSearchResult> results = new ArrayList<>();
            for (JsonNode item : data) {
                String url = item.path("url").asText();
                if (!url.isBlank()) results.add(new WebSearchResult(item.path("title").asText(url), url, item.path("description").asText(item.path("markdown").asText())));
            }
            return List.copyOf(results);
        } catch (Exception exception) { throw new IllegalStateException("Firecrawl 搜索响应解析失败。", exception); }
    }
}
