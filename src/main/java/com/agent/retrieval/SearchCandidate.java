package com.agent.retrieval;

import com.agent.document.Chunk;

public record SearchCandidate(Chunk chunk, double score, SearchSource source) {
}
