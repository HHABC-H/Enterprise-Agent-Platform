package com.agent.chat;

import java.util.List;

public interface WebSearchTool {
    List<WebSearchResult> search(String question);
}
