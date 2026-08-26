package com.agent.auth;

import com.agent.config.AgentPlatformProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 使用项目配置的 HMAC 密钥签发 JWT，不依赖外部 issuer。 */
@Component
public class JwtService {
    private final AgentPlatformProperties properties;
    private final Clock clock;

    public JwtService(AgentPlatformProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public LoginResult issue(UserAccount account) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plusSeconds(properties.getAuth().getJwtTtlSeconds());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(account.username())
                .claim("tenant_id", account.tenantId())
                .claim("roles", account.roles().stream().map(Enum::name).sorted().toList())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secretBytes()));
            return new LoginResult(jwt.serialize(), expiresAt, account.username(), account.tenantId(), account.roles());
        } catch (JOSEException exception) {
            throw new IllegalStateException("JWT 签发失败。", exception);
        }
    }

    public byte[] secretBytes() {
        byte[] secret = properties.getAuth().getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET 至少需要 32 个字节。");
        }
        return secret;
    }
}
