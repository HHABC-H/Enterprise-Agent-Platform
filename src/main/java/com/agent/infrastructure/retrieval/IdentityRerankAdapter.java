package com.agent.infrastructure.retrieval;

import com.agent.retrieval.RerankPort;
import com.agent.retrieval.RetrievalEvidence;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IdentityRerankAdapter implements RerankPort {
    @Override
    public List<RetrievalEvidence> rerank(String question, List<RetrievalEvidence> evidence) { return evidence; }
}
