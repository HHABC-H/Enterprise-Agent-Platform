package com.agent.evaluation;

/** Java 本地可验证指标；Ragas 指标另以状态表示，未配置时不伪造数值。 */
public record EvaluationSummary(int total, int succeeded, int failed, int evidenceHits, int correctRejects,
                                double answerMatchRate, double averageDurationMs, String ragasStatus) { }
