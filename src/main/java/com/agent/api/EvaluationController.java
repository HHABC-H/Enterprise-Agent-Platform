/**
 * 本文件定义 {@code EvaluationController}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import com.agent.evaluation.EvaluationResult;
import com.agent.evaluation.EvaluationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    private final EvaluationService evaluationService;
    public EvaluationController(EvaluationService evaluationService) { this.evaluationService = evaluationService; }
    @PostMapping("/run")
    public ApiResponse<EvaluationResult> run() { return ApiResponse.of(evaluationService.run()); }
}
