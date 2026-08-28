package com.agent.memory;

import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private final UserProfileStore store;
    private final Clock clock;
    public UserProfileService(UserProfileStore store, Clock clock) { this.store = store; this.clock = clock; }
    public UserProfile get(String tenantId, String userId) {
        try { return store.get(tenantId, userId); }
        catch (RuntimeException exception) { log.warn("用户画像读取失败，已降级为空画像: {}", exception.getClass().getSimpleName()); return UserProfile.empty(); }
    }
    public void merge(String tenantId, String userId, UserProfile incoming) {
        try {
            UserProfile current = store.get(tenantId, userId);
            store.save(tenantId, userId, new UserProfile(value(incoming.preferredLanguage(), current.preferredLanguage()), value(incoming.codingStyle(), current.codingStyle()),
                    incoming.techStack().isEmpty() ? current.techStack() : List.copyOf(incoming.techStack()), value(incoming.projectBudget(), current.projectBudget()),
                    value(incoming.role(), current.role()), clock.instant()));
        } catch (RuntimeException exception) { log.warn("用户画像更新失败，主对话继续执行: {}", exception.getClass().getSimpleName()); }
    }
    private String value(String preferred, String fallback) { return preferred == null || preferred.isBlank() ? fallback : preferred; }
}
