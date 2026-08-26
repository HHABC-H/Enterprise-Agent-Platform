package com.agent.infrastructure.auth;

import com.agent.auth.PlatformRole;
import com.agent.auth.UserAccount;
import com.agent.auth.UserRepository;
import com.agent.auth.UsernameAlreadyExistsException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** docker profile 的 PostgreSQL 用户仓储，依赖 Flyway 创建的 platform_user 表。 */
@Component
@Profile({"docker", "local-docker"})
public class JdbcUserRepository implements UserRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcUserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserAccount create(UserAccount account) {
        try {
            jdbc.update("INSERT INTO platform_user (id, username, password_hash, tenant_id, roles, created_at) "
                            + "VALUES (:id, :username, :passwordHash, :tenantId, CAST(:roles AS text[]), :createdAt)",
                    Map.of("id", account.id(), "username", account.username(), "passwordHash", account.passwordHash(),
                            "tenantId", account.tenantId(), "roles", postgresArray(account.roles()),
                            "createdAt", Timestamp.from(account.createdAt())));
            return account;
        } catch (DuplicateKeyException exception) {
            throw new UsernameAlreadyExistsException();
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return jdbc.query("SELECT id, username, password_hash, tenant_id, roles, created_at FROM platform_user WHERE username = :username",
                        Map.of("username", username), (resultSet, row) -> new UserAccount(resultSet.getString("id"),
                                resultSet.getString("username"), resultSet.getString("password_hash"), resultSet.getString("tenant_id"),
                                roles(resultSet.getArray("roles")), resultSet.getTimestamp("created_at").toInstant()))
                .stream().findFirst();
    }

    private String postgresArray(java.util.Set<PlatformRole> roles) {
        return "{" + roles.stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(",")) + "}";
    }

    private java.util.Set<PlatformRole> roles(java.sql.Array value) throws java.sql.SQLException {
        if (value == null || !(value.getArray() instanceof String[] entries)) {
            return EnumSet.of(PlatformRole.USER);
        }
        return Arrays.stream(entries).map(PlatformRole::valueOf).collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(PlatformRole.class)));
    }
}
