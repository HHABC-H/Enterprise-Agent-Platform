package com.agent.infrastructure.memory;

import com.agent.memory.UserProfile;
import com.agent.memory.UserProfileStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai-platform.memory", name = "type", havingValue = "redis")
public class RedisUserProfileStore implements UserProfileStore {
    private final StringRedisTemplate redis;
    public RedisUserProfileStore(StringRedisTemplate redis) { this.redis = redis; }
    @Override public UserProfile get(String tenantId, String userId) {
        Map<Object, Object> values = redis.opsForHash().entries(key(tenantId, userId));
        if (values.isEmpty()) return UserProfile.empty();
        try { return new UserProfile(value(values, "preferredLanguage"), value(values, "codingStyle"), split(value(values, "techStack")), value(values, "projectBudget"), value(values, "role"), Instant.parse(value(values, "updatedAt"))); }
        catch (RuntimeException exception) { throw new IllegalStateException("用户画像读取失败。", exception); }
    }
    @Override public void save(String tenantId, String userId, UserProfile profile) {
        try { redis.opsForHash().putAll(key(tenantId, userId), Map.of("preferredLanguage", profile.preferredLanguage(), "codingStyle", profile.codingStyle(), "techStack", String.join("\u001f", profile.techStack()), "projectBudget", profile.projectBudget(), "role", profile.role(), "updatedAt", profile.updatedAt().toString())); }
        catch (RuntimeException exception) { throw new IllegalStateException("用户画像写入失败。", exception); }
    }
    private String key(String tenantId, String userId) { return "user:profile:" + tenantId + ':' + userId; }
    private String value(Map<Object, Object> values, String key) { Object value = values.get(key); return value == null ? "" : value.toString(); }
    private List<String> split(String value) { return value.isBlank() ? List.of() : List.of(value.split("\u001f")); }
}
