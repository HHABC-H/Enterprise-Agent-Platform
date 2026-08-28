package com.agent.infrastructure.memory;

import com.agent.config.AgentPlatformProperties;
import com.agent.memory.MemoryEntry;
import com.agent.memory.StructuredProfileExtractionPort;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 显式启用后要求模型只输出画像 JSON。 */
@Component
@ConditionalOnProperty(prefix = "ai-platform.memory.profile-extraction", name = "llm-enabled", havingValue = "true")
public class LangChain4jStructuredProfileExtractionAdapter implements StructuredProfileExtractionPort {
    private final OpenAiChatModel model;
    public LangChain4jStructuredProfileExtractionAdapter(AgentPlatformProperties properties) { var builder = OpenAiChatModel.builder().apiKey(properties.getLlm().getApiKey()).modelName(properties.getLlm().getModelName()); if (properties.getLlm().getBaseUrl() != null && !properties.getLlm().getBaseUrl().isBlank()) builder.baseUrl(properties.getLlm().getBaseUrl()); model = builder.build(); }
    @Override public String extractJson(List<MemoryEntry> entries) {
        String conversation = entries.stream().filter(item -> "user".equals(item.role())).map(item -> item.content()).reduce("", (left, right) -> left + "\n" + right);
        return model.chat("从对话提取用户画像，只输出 JSON：{\"preferredLanguage\":\"\",\"codingStyle\":\"\",\"techStack\":[],\"projectBudget\":\"\",\"role\":\"\"}。未知字段留空。\n对话：" + conversation);
    }
}
