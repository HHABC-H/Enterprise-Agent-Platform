package com.agent.infrastructure.memory;

import com.agent.config.AgentPlatformProperties;
import com.agent.memory.MemoryEntry;
import com.agent.memory.MemorySummarizationPort;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 显式配置后使用大模型生成中文摘要。 */
@Component
@ConditionalOnProperty(prefix = "ai-platform.memory.summarization", name = "llm-enabled", havingValue = "true")
public class LangChain4jMemorySummarizationAdapter implements MemorySummarizationPort {
    private final OpenAiChatModel model;
    public LangChain4jMemorySummarizationAdapter(AgentPlatformProperties properties) {
        var builder = OpenAiChatModel.builder().apiKey(properties.getLlm().getApiKey()).modelName(properties.getLlm().getModelName());
        if (properties.getLlm().getBaseUrl() != null && !properties.getLlm().getBaseUrl().isBlank()) builder.baseUrl(properties.getLlm().getBaseUrl());
        model = builder.build();
    }
    @Override public String summarize(List<MemoryEntry> entries, int maximumCharacters) {
        String conversation = entries.stream().map(item -> item.role() + "：" + item.content()).reduce("", (left, right) -> left + "\n" + right);
        String result = model.chat("请将以下对话总结为不超过 " + maximumCharacters + " 个中文字符的事实摘要，不要编造信息：\n" + conversation);
        return result.length() > maximumCharacters ? result.substring(0, maximumCharacters) : result;
    }
}
