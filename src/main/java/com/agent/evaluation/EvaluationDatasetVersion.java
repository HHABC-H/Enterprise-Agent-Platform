package com.agent.evaluation;

import java.time.Instant;

public record EvaluationDatasetVersion(String id, String tenantId, String datasetId, int version, String name,
                                       String createdBy, Instant createdAt, long rowVersion) { }
