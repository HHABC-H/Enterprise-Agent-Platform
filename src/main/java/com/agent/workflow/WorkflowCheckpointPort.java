package com.agent.workflow;

import java.util.List;

public interface WorkflowCheckpointPort {
    void save(String sessionId, List<WorkflowState> states);
}
