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
