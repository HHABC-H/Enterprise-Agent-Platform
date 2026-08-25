/**
 * 本文件定义 {@code ChatService}，负责对话服务与回答生成逻辑。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
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
        this.workflowService = workflowService; this.answerGenerator = answerGenerator; this.memoryManager = memoryManager;
    }
    public ChatResult chat(String tenantId, String userId, String sessionId, String question) { return chat(tenantId, userId, sessionId, question, false); }
    public ChatResult chat(String tenantId, String userId, String sessionId, String question, boolean requireApproval) {
        memoryManager.append(sessionId, "user", question);
        WorkflowResult workflow = workflowService.execute(tenantId, userId, sessionId, question, requireApproval);
        if (workflow.waitingApproval()) { return new ChatResult(null, false, null, workflow.searchResponse().evidence(), workflow.trace(), workflow.workflowId(), true); }
        if (!workflow.searchResponse().decision().sufficient()) {
            String reason = workflow.searchResponse().decision().refusalReason(); memoryManager.append(sessionId, "assistant", reason);
            return new ChatResult(null, true, reason, workflow.searchResponse().evidence(), workflow.trace(), workflow.workflowId(), false);
        }
        String answer = answerGenerator.generate(question, workflow.searchResponse().evidence()); memoryManager.append(sessionId, "assistant", answer);
        return new ChatResult(answer, false, null, workflow.searchResponse().evidence(), workflow.trace(), workflow.workflowId(), false);
    }
}
