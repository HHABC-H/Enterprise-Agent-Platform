package com.agent.api;

import com.agent.memory.ChatSession;
import com.agent.memory.ChatSessionService;
import com.agent.memory.ShortTermMemoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/sessions")
public class ChatSessionController {
    private final IdentityGuard identityGuard;
    private final ChatSessionService sessions;
    private final ShortTermMemoryService shortTermMemory;
    private final com.agent.memory.SessionSummarizer summarizer;
    public ChatSessionController(IdentityGuard identityGuard, ChatSessionService sessions, ShortTermMemoryService shortTermMemory, com.agent.memory.SessionSummarizer summarizer) {
        this.identityGuard = identityGuard;
        this.sessions = sessions;
        this.shortTermMemory = shortTermMemory;
        this.summarizer = summarizer;
    }
    @PostMapping
    public ApiResponse<ChatSession> create() {
        IdentityGuard.Actor actor = identityGuard.actor();
        return ApiResponse.of(sessions.create(actor.tenantId(), actor.userId()));
    }
    @GetMapping
    public ApiResponse<List<ChatSession>> list() {
        IdentityGuard.Actor actor = identityGuard.actor();
        return ApiResponse.of(sessions.list(actor.tenantId(), actor.userId()));
    }
    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<com.agent.memory.MemoryEntry>> messages(@PathVariable String sessionId) {
        IdentityGuard.Actor actor = identityGuard.actor();
        return ApiResponse.of(shortTermMemory.read(actor.tenantId(), actor.userId(), sessionId));
    }
    @PostMapping("/{sessionId}/close")
    public ApiResponse<Void> close(@PathVariable String sessionId) {
        IdentityGuard.Actor actor = identityGuard.actor();
        if (!sessions.close(actor.tenantId(), actor.userId(), sessionId)) {
            throw new IllegalArgumentException("会话不存在或无权关闭。");
        }
        summarizer.summarizeNow(actor.tenantId(), actor.userId(), sessionId);
        return ApiResponse.of(null);
    }
}
