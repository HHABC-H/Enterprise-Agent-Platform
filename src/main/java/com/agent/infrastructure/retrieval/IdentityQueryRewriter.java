/**
 * 本文件定义 {@code IdentityQueryRewriter}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.retrieval;

import com.agent.retrieval.QueryRewriter;
import org.springframework.stereotype.Component;

@Component
public class IdentityQueryRewriter implements QueryRewriter {
    @Override
    public String rewrite(String question) { return question; }
}
