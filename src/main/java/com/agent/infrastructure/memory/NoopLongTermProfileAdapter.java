package com.agent.infrastructure.memory;

import com.agent.memory.LongTermProfilePort;
import org.springframework.stereotype.Component;

@Component
public class NoopLongTermProfileAdapter implements LongTermProfilePort {
    @Override
    public void saveVerified(String tenantId, String userId, String field, String value) {
    }
}
