package com.agent.auth;

import java.time.Instant;
import java.util.Set;

/** 登录成功后返回的访问令牌及其可安全展示的声明。 */
public record LoginResult(String accessToken, Instant expiresAt, String username, String tenantId, Set<PlatformRole> roles) {
}
