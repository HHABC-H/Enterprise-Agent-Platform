/**
 * 本文件定义 {@code AnswerGenerator}，负责对话服务与回答生成逻辑。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.chat;

import com.agent.retrieval.RetrievalEvidence;
import com.agent.memory.MemoryEntry;
import java.util.List;

public interface AnswerGenerator {
    String generate(String question, List<RetrievalEvidence> evidence);

    default String generate(String question, List<RetrievalEvidence> evidence, List<MemoryEntry> conversation) {
        return generate(question, evidence);
    }

    default String generate(ChatContext context) {
        return generate(context.question(), context.evidence(), context.conversation());
    }
}
