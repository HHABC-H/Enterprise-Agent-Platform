package com.agent.infrastructure.chat;

import com.agent.chat.WebSearchResult;
import com.agent.chat.WebSearchTool;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai-platform.web-search", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopWebSearchTool implements WebSearchTool {
    @Override public List<WebSearchResult> search(String question) { return List.of(); }
}
