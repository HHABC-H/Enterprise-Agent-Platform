package com.agent.chat;

import com.agent.memory.MemoryManager;
import com.agent.workflow.AgentWorkflowService;
import com.agent.workflow.WorkflowResult;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final AgentWorkflowService workflowService;
    private final AnswerGenerator answerGenerator;
    private final MemoryManager memoryManager;
    public ChatService(AgentWorkflowService workflowService, AnswerGenerator answerGenerator, MemoryManager memoryManager) {
        this.workflowService = workflowService;
        this.answerGenerator = answerGenerator;
        this.memoryManager = memoryManager;
    }
    public ChatResult chat(String tenantId, String userId, String sessionId, String question) {
        memoryManager.append(sessionId, "user", question);
        WorkflowResult workflow = workflowService.execute(tenantId, userId, sessionId, question);
        if (!workflow.searchResponse().decision().sufficient()) {
            String reason = workflow.searchResponse().decision().refusalReason();
            memoryManager.append(sessionId, "assistant", reason);
            return new ChatResult(null, true, reason, workflow.searchResponse().evidence(), workflow.trace());
        }
        String answer = answerGenerator.generate(question, workflow.searchResponse().evidence());
        memoryManager.append(sessionId, "assistant", answer);
        return new ChatResult(answer, false, null, workflow.searchResponse().evidence(), workflow.trace());
    }
}
