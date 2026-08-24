package com.agent.infrastructure.retrieval;

import com.agent.retrieval.QueryRewriter;
import org.springframework.stereotype.Component;

@Component
public class IdentityQueryRewriter implements QueryRewriter {
    @Override
    public String rewrite(String question) { return question; }
}
