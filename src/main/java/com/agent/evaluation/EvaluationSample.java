package com.agent.evaluation;

import java.util.Set;

/** 不可变数据集版本中的评测样例。 */
public record EvaluationSample(String id, String question, String expectedAnswer, String type,
                               String expectedEvidence, boolean expectReject, Set<String> tags, String fingerprint) {
    public EvaluationSample { tags = tags == null ? Set.of() : Set.copyOf(tags); }
}
