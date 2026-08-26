package com.agent.evaluation;

import java.time.Instant;

public record BadCase(String id, String tenantId, String sourceRunId, String sourceSampleId, String stableKey,
                      String failureCategory, String severity, BadCaseStatus status, String ownerNote,
                      Instant createdAt, Instant updatedAt, long rowVersion) { }
