package com.agent.retrieval;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class TextTokens {
    private TextTokens() {
    }

    static Set<String> tokenize(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        String normalized = value.toLowerCase(Locale.ROOT);
        StringBuilder latin = new StringBuilder();
        normalized.codePoints().forEach(codePoint -> {
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                flush(latin, tokens);
                tokens.add(new String(Character.toChars(codePoint)));
            } else if (Character.isLetterOrDigit(codePoint)) {
                latin.appendCodePoint(codePoint);
            } else {
                flush(latin, tokens);
            }
        });
        flush(latin, tokens);
        return tokens;
    }

    static double overlapRatio(String question, String content) {
        Set<String> queryTokens = tokenize(question);
        if (queryTokens.isEmpty()) {
            return 0;
        }
        Set<String> contentTokens = tokenize(content);
        long matches = queryTokens.stream().filter(contentTokens::contains).count();
        return (double) matches / queryTokens.size();
    }

    private static void flush(StringBuilder token, Set<String> target) {
        if (!token.isEmpty()) {
            target.add(token.toString());
            token.setLength(0);
        }
    }
}
