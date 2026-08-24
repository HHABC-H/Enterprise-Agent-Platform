package com.agent.chat;

import com.agent.retrieval.RetrievalEvidence;
import java.util.List;

public interface AnswerGenerator {
    String generate(String question, List<RetrievalEvidence> evidence);
}
