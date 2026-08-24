package com.agent.evaluation;

public record EvaluationResult(int total, int evidenceHits, int correctRejects, double answerMatchRate, double averageDurationMs) {
}
