package com.agent.memory;

public interface UserProfileStore {
    UserProfile get(String tenantId, String userId);
    void save(String tenantId, String userId, UserProfile profile);
}
