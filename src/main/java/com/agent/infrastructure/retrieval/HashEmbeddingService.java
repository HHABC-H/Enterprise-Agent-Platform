/**
 * 本文件定义 {@code HashEmbeddingService}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.retrieval;

import com.agent.retrieval.EmbeddingService;
import com.agent.retrieval.TextVectorSupport;
import org.springframework.stereotype.Component;

@Component
public class HashEmbeddingService implements EmbeddingService {
    @Override
    public double similarity(String left, String right) {
        return TextVectorSupport.cosine(left, right);
    }
}
