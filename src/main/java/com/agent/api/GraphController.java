/**
 * 本文件定义 {@code GraphController}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import com.agent.extension.GraphRelationQuery;
import com.agent.extension.GraphRelationSearchPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graph")
public class GraphController {
    private final GraphRelationSearchPort graph;
    private final IdentityGuard identityGuard;
    public GraphController(GraphRelationSearchPort graph, IdentityGuard identityGuard) { this.graph = graph; this.identityGuard = identityGuard; }
    @GetMapping("/relations")
    public ApiResponse<GraphRelationResponse> relations(@RequestParam String tenantId, @RequestParam String userId,
                                                         @RequestParam String documentId, @RequestParam String version,
                                                         @RequestParam(defaultValue = "1") int maxHops,
                                                         @RequestParam(defaultValue = "20") int limit) {
        identityGuard.assertRequestIdentity(tenantId, userId);
        return ApiResponse.of(new GraphRelationResponse(graph.available(), graph.search(new GraphRelationQuery(tenantId, userId, documentId, version, maxHops, limit))));
    }
}
