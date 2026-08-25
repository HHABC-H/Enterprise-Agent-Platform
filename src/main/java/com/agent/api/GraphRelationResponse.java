/**
 * 本文件定义 {@code GraphRelationResponse}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import com.agent.extension.GraphRelation;
import java.util.List;

public record GraphRelationResponse(boolean graphAvailable, List<GraphRelation> relations) { }
