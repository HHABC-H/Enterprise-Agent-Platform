package com.agent.auth;

import java.time.Instant;
import java.util.Set;

/** 已注册用户的最小持久化模型，密码仅保存 BCrypt 哈希。 */
public record UserAccount(String id, String username, String passwordHash, String tenantId,
                          Set<PlatformRole> roles, Instant createdAt) {
}
