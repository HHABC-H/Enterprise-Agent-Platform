/**
 * 本文件定义 {@code TextVectorSupport}，负责检索、权限过滤、证据校验与排序流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
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
