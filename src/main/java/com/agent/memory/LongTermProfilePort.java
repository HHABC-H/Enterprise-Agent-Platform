package com.agent.memory;

public interface LongTermProfilePort {
    void saveVerified(String tenantId, String userId, String field, String value);
}
