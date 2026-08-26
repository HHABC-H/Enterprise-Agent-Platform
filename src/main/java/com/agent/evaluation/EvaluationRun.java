package com.agent.evaluation;

import java.time.Instant;

public record EvaluationRun(String id, String tenantId, String datasetVersionId, EvaluationRunState state,
                            String requestedBy, String codeVersion, String configVersion, String modelName,
                            String promptHash, Instant createdAt, Instant startedAt, Instant finishedAt,
                            String failureCategory, EvaluationSummary summary, long rowVersion) { }
