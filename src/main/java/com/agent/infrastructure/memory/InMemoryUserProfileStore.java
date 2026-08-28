package com.agent.infrastructure.memory;

import com.agent.memory.UserProfile;
import com.agent.memory.UserProfileStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai-platform.memory", name = "type", havingValue = "memory", matchIfMissing = true)
public class InMemoryUserProfileStore implements UserProfileStore {
    private final Map<String, UserProfile> values = new ConcurrentHashMap<>();
    @Override public UserProfile get(String tenantId, String userId) { return values.getOrDefault(key(tenantId, userId), UserProfile.empty()); }
    @Override public void save(String tenantId, String userId, UserProfile profile) { values.put(key(tenantId, userId), profile); }
    private String key(String tenantId, String userId) { return tenantId + ':' + userId; }
}
