package com.agent.infrastructure.workflow;

import com.agent.workflow.WorkflowCheckpointPort;
import com.agent.workflow.WorkflowState;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryWorkflowCheckpointAdapter implements WorkflowCheckpointPort {
    private final ConcurrentHashMap<String, List<WorkflowState>> checkpoints = new ConcurrentHashMap<>();
    @Override
    public void save(String sessionId, List<WorkflowState> states) { checkpoints.put(sessionId, List.copyOf(states)); }
}
