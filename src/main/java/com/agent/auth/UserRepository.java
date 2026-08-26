package com.agent.auth;

import java.util.Optional;

/** 用户存储端口；local 使用内存实现，docker 使用 PostgreSQL 实现。 */
public interface UserRepository {
    UserAccount create(UserAccount account);

    Optional<UserAccount> findByUsername(String username);
}
