package com.agent.retrieval;

import java.util.Set;

public final class TextVectorSupport {
    private TextVectorSupport() {
    }

    public static double cosine(String left, String right) {
        Set<String> leftTokens = TextTokens.tokenize(left);
        Set<String> rightTokens = TextTokens.tokenize(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }
        long intersection = leftTokens.stream().filter(rightTokens::contains).count();
        return intersection / Math.sqrt((double) leftTokens.size() * rightTokens.size());
    }
}
