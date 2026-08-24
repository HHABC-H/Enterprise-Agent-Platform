package com.agent.infrastructure.chat;

import com.agent.chat.AnswerGenerator;
import com.agent.config.AgentPlatformProperties;
import com.agent.retrieval.RetrievalEvidence;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai-platform.llm", name = "enabled", havingValue = "true")
public class LangChain4jAnswerGenerator implements AnswerGenerator {
    private final ChatModel chatModel;
    public LangChain4jAnswerGenerator(AgentPlatformProperties properties) {
        if (isBlank(properties.getLlm().getApiKey()) || isBlank(properties.getLlm().getModelName())) {
            throw new IllegalStateException("启用 LLM 时必须配置 AI_PLATFORM_LLM_API_KEY 和 AI_PLATFORM_LLM_MODEL_NAME。");
        }
        var builder = OpenAiChatModel.builder().apiKey(properties.getLlm().getApiKey())
                .modelName(properties.getLlm().getModelName());
        if (!isBlank(properties.getLlm().getBaseUrl())) { builder.baseUrl(properties.getLlm().getBaseUrl()); }
        chatModel = builder.build();
    }
    @Override
    public String generate(String question, List<RetrievalEvidence> evidence) {
        String context = evidence.stream().map(item -> item.chunk().content()).reduce("", (left, right) -> left + "\n---\n" + right);
        return chatModel.chat("只依据以下证据用中文回答；若证据不足请回答无法可靠回答。\n问题：" + question + "\n证据：\n" + context);
    }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
