/**
 * 本文件定义 {@code RedisMemoryStore}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.memory;

import com.agent.memory.MemoryEntry;
import com.agent.memory.MemoryStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai-platform.memory", name = "type", havingValue = "redis")
public class RedisMemoryStore implements MemoryStore {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final org.springframework.data.redis.core.script.DefaultRedisScript<Long> APPEND_TURN = new org.springframework.data.redis.core.script.DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); local messages = {}; if value then messages = cjson.decode(value); end; "
                    + "table.insert(messages, cjson.decode(ARGV[1])); table.insert(messages, cjson.decode(ARGV[2])); "
                    + "while #messages > tonumber(ARGV[3]) do table.remove(messages, 1); end; "
                    + "redis.call('SET', KEYS[1], cjson.encode(messages), 'EX', tonumber(ARGV[4])); return #messages;", Long.class);
    public RedisMemoryStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    @Override
    public List<MemoryEntry> read(String sessionId) {
        String value = redisTemplate.opsForValue().get(key(sessionId));
        if (value == null) { return List.of(); }
        try { return objectMapper.readValue(value, new TypeReference<List<MemoryEntry>>() { }); }
        catch (Exception exception) { throw new IllegalStateException("会话记忆读取失败。", exception); }
    }
    @Override
    public void appendTurn(String sessionId, MemoryEntry userEntry, MemoryEntry assistantEntry, int maxMessages, Duration ttl) {
        try { redisTemplate.execute(APPEND_TURN, List.of(key(sessionId)), objectMapper.writeValueAsString(userEntry),
                objectMapper.writeValueAsString(assistantEntry), Integer.toString(maxMessages), Long.toString(ttl.toSeconds())); }
        catch (Exception exception) { throw new IllegalStateException("会话记忆写入失败。", exception); }
    }
    @Override
    public void replace(String sessionId, List<MemoryEntry> entries, Duration ttl) {
        try { redisTemplate.opsForValue().set(key(sessionId), objectMapper.writeValueAsString(entries), ttl); }
        catch (Exception exception) { throw new IllegalStateException("会话记忆写入失败。", exception); }
    }
    private String key(String sessionId) { return "session:" + sessionId; }
}
