package com.agent.evaluation;

public record EvaluationCaseResult(String id, String tenantId, String runId, String sampleId, boolean passed,
                                   String failureCategory, double durationMs, boolean evidenceHit, boolean refused,
                                   String ragasStatus) { }
