package com.interval.auth;

/**
 * 认证失败异常
 * 
 * 当用户名或密码错误时抛出
 */
public class AuthenticationFailedException extends RuntimeException {
    
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
