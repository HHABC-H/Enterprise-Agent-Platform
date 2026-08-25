/**
 * 本文件定义 {@code EvaluationResult}，负责评测用例、评测结果与评测服务。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.evaluation;

public record EvaluationResult(int total, int evidenceHits, int correctRejects, double answerMatchRate, double averageDurationMs) {
}
