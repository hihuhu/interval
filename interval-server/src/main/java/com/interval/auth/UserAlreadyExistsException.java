package com.interval.auth;

/**
 * 用户已存在异常
 * 
 * 当尝试注册已存在的用户名时抛出
 */
public class UserAlreadyExistsException extends RuntimeException {
    
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
