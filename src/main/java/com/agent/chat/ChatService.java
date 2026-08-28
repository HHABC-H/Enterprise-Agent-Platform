/**
 * 本文件定义 {@code ChatService}，负责对话服务与回答生成逻辑。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.chat;

import com.agent.memory.MemoryEntry;
import com.agent.memory.ShortTermMemoryService;
import com.agent.memory.LongTermMemoryService;
import com.agent.memory.UserProfileService;
import com.agent.memory.ChatSessionService;
import com.agent.workflow.AgentWorkflowService;
import com.agent.workflow.WorkflowResult;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatService {
    private final AgentWorkflowService workflowService;
    private final AnswerGenerator answerGenerator;
    private final ShortTermMemoryService shortTermMemory;
    private final LongTermMemoryService longTermMemory;
    private final UserProfileService profiles;
    private final ChatSessionService sessions;
    private final WebSearchTool webSearchTool;
    public ChatService(AgentWorkflowService workflowService, AnswerGenerator answerGenerator, ShortTermMemoryService shortTermMemory,
                       LongTermMemoryService longTermMemory, UserProfileService profiles, ChatSessionService sessions, WebSearchTool webSearchTool) {
        this.workflowService = workflowService; this.answerGenerator = answerGenerator; this.shortTermMemory = shortTermMemory;
        this.longTermMemory = longTermMemory; this.profiles = profiles; this.sessions = sessions; this.webSearchTool = webSearchTool;
    }
    public ChatResult chat(String tenantId, String userId, String sessionId, String question) {
        return chat(tenantId, userId, sessionId, question, false);
    }
    public ChatResult chat(String tenantId, String userId, String sessionId, String question, boolean webSearchEnabled) {
        return chat(tenantId, userId, sessionId, question, false, webSearchEnabled);
    }
    public ChatResult chat(String tenantId, String userId, String sessionId, String question, boolean requireApproval, boolean webSearchEnabled) {
        List<MemoryEntry> conversation = shortTermMemory.read(tenantId, userId, sessionId);
        sessions.touch(tenantId, userId, sessionId, question);
        WorkflowResult workflow = workflowService.execute(tenantId, userId, sessionId, question, requireApproval);
        if (workflow.waitingApproval()) {
            return new ChatResult(null, false, null, workflow.searchResponse().evidence(), workflow.trace(), workflow.workflowId(), true, List.of());
        }
        List<WebSearchResult> webResults = webSearchEnabled ? safeWebSearch(question) : List.of();
        String answer = answerGenerator.generate(new ChatContext(question, conversation, profiles.get(tenantId, userId),
                longTermMemory.retrieve(tenantId, userId, question), workflow.searchResponse().evidence(), webResults));
        shortTermMemory.appendTurn(tenantId, userId, sessionId, question, answer);
        return new ChatResult(answer, false, null, workflow.searchResponse().evidence(), workflow.trace(), workflow.workflowId(), false, webResults);
    }
    private List<WebSearchResult> safeWebSearch(String question) {
        try { return webSearchTool.search(question); }
        catch (RuntimeException exception) { return List.of(); }
    }
}
