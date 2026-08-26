package com.agent.infrastructure.auth;

import com.agent.auth.UserAccount;
import com.agent.auth.UserRepository;
import com.agent.auth.UsernameAlreadyExistsException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local profile 的用户仓储，用于不连接外部数据库时的完整认证链路验证。 */
@Component
@Profile("local")
public class InMemoryUserRepository implements UserRepository {
    private final ConcurrentHashMap<String, UserAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public UserAccount create(UserAccount account) {
        if (accounts.putIfAbsent(account.username(), account) != null) {
            throw new UsernameAlreadyExistsException();
        }
        return account;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(accounts.get(username));
    }
}
