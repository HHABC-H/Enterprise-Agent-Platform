package com.agent.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** 处理注册、密码校验和本地 JWT 签发，不向调用方返回密码哈希。 */
@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    public UserAccount register(String username, String password, String tenantId) {
        String normalizedUsername = normalizeUsername(username);
        UserAccount account = new UserAccount(UUID.randomUUID().toString(), normalizedUsername, passwordEncoder.encode(password),
                tenantId.trim(), rolesFor(normalizedUsername), Instant.now(clock));
        return users.create(account);
    }

    public LoginResult login(String username, String password) {
        UserAccount account = users.findByUsername(normalizeUsername(username)).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, account.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return jwtService.issue(account);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /** 复用既有审批人白名单，避免注册接口接受客户端提交的提权角色。 */
    private Set<PlatformRole> rolesFor(String username) {
        boolean approver = Arrays.stream(System.getenv().getOrDefault("AI_PLATFORM_WORKFLOW_APPROVER_IDS", "reviewer").split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT)).anyMatch(username::equals);
        return approver ? Set.of(PlatformRole.USER, PlatformRole.APPROVER) : Set.of(PlatformRole.USER);
    }
}
