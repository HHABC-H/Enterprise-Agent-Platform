package com.agent.extension;

import java.util.List;

public interface GraphRelationSearchPort {
    List<String> findRelatedChunks(String documentId, int maxHops);
}
