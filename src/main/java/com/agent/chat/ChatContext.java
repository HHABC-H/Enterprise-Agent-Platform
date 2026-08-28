package com.agent.chat;

import com.agent.memory.LongTermMemory;
import com.agent.memory.MemoryEntry;
import com.agent.memory.UserProfile;
import com.agent.retrieval.RetrievalEvidence;
import java.util.List;

public record ChatContext(String question, List<MemoryEntry> conversation, UserProfile profile,
                          List<LongTermMemory> longTermMemories, List<RetrievalEvidence> evidence,
                          List<WebSearchResult> webResults) {
    public ChatContext {
        conversation = List.copyOf(conversation);
        longTermMemories = List.copyOf(longTermMemories);
        evidence = List.copyOf(evidence);
        webResults = List.copyOf(webResults);
    }
}
