package com.agent.memory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionSummarizer {
    private static final Logger log = LoggerFactory.getLogger(SessionSummarizer.class);
    private final ChatSessionService sessions;
    private final ShortTermMemoryService shortTermMemory;
    private final LongTermMemoryService longTermMemory;
    private final StructuredMemoryExtractor extractor;
    private final UserProfileService profiles;
    private final MemorySummarizationPort summarizationPort;
    public SessionSummarizer(ChatSessionService sessions, ShortTermMemoryService shortTermMemory, LongTermMemoryService longTermMemory,
                             StructuredMemoryExtractor extractor, UserProfileService profiles, MemorySummarizationPort summarizationPort) {
        this.sessions = sessions;
        this.shortTermMemory = shortTermMemory;
        this.longTermMemory = longTermMemory;
        this.extractor = extractor;
        this.profiles = profiles;
        this.summarizationPort = summarizationPort;
    }
    @Scheduled(cron = "${ai-platform.memory.schedule.session-summary:0 0 */3 * * *}")
    public void summarizeActiveSessions() {
        for (ChatSessionStore.SessionCandidate session : sessions.pendingSummary()) summarize(session);
    }
    @Scheduled(cron = "${ai-platform.memory.schedule.cleanup:0 0 2 * * *}")
    public void cleanupExpiredMemories() { longTermMemory.cleanup(); }
    public void summarize(ChatSessionStore.SessionCandidate session) {
        try {
            List<MemoryEntry> entries = shortTermMemory.read(session.tenantId(), session.userId(), session.session().sessionId());
            if (entries.isEmpty()) return;
            String content = summary(entries);
            longTermMemory.save(session.tenantId(), session.userId(), session.session().sessionId(), content, MemoryType.EPISODIC, 0.7);
            profiles.merge(session.tenantId(), session.userId(), extractor.extract(entries));
            sessions.markSummarized(session);
            log.info("会话 {} 已完成记忆摘要", session.session().sessionId());
        } catch (RuntimeException exception) { log.warn("会话摘要失败: {}", exception.getClass().getSimpleName()); }
    }
    public void summarizeNow(String tenantId, String userId, String sessionId) { sessions.candidate(tenantId, userId, sessionId).ifPresent(this::summarize); }
    private String summary(List<MemoryEntry> entries) {
        return summarizationPort.summarize(entries, 200);
    }
}
