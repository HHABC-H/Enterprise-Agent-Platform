package com.agent.retrieval;

import com.agent.document.Chunk;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RrfFusion {
    private RrfFusion() { }
    public static List<RetrievalEvidence> fuse(List<SearchCandidate> vector, List<SearchCandidate> fullText, int k) {
        Map<String, FusionItem> fused = new HashMap<>();
        addRanks(fused, vector, k);
        addRanks(fused, fullText, k);
        return fused.values().stream().map(item -> new RetrievalEvidence(item.chunk, item.score, item.sources))
                .sorted(Comparator.comparingDouble(RetrievalEvidence::score).reversed()).toList();
    }
    private static void addRanks(Map<String, FusionItem> fused, List<SearchCandidate> candidates, int k) {
        for (int index = 0; index < candidates.size(); index++) {
            SearchCandidate candidate = candidates.get(index);
            FusionItem item = fused.computeIfAbsent(candidate.chunk().chunkId(), key -> new FusionItem(candidate.chunk()));
            item.score += 1.0 / (k + index + 1);
            item.sources.add(candidate.source());
        }
    }
    private static final class FusionItem {
        private final Chunk chunk;
        private final Set<SearchSource> sources = new java.util.HashSet<>();
        private double score;
        private FusionItem(Chunk chunk) { this.chunk = chunk; }
    }
}
