package com.agent.auth;

/** 登录失败统一使用该异常，避免通过错误信息枚举用户名。 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("用户名或密码错误。");
    }
}
