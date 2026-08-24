package com.agent.chat;

import com.agent.retrieval.RetrievalEvidence;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai-platform.llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class EvidenceBoundTemplateAnswerGenerator implements AnswerGenerator {
    @Override
    public String generate(String question, List<RetrievalEvidence> evidence) {
        RetrievalEvidence top = evidence.get(0);
        String content = top.chunk().content().length() > 800 ? top.chunk().content().substring(0, 800) + "…" : top.chunk().content();
        return "根据已授权文档检索到的证据：\n" + content;
    }
}
