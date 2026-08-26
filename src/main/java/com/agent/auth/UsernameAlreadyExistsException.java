package com.agent.auth;

/** 用户名已经存在时抛出，避免把底层唯一索引错误暴露给接口调用方。 */
public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("用户名已被注册。");
    }
}
