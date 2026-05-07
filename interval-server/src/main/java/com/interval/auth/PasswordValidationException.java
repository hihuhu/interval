package com.interval.auth;

/**
 * 密码验证失败异常
 * 
 * 当密码不符合强度要求时抛出
 */
public class PasswordValidationException extends RuntimeException {
    
    public PasswordValidationException(String message) {
        super(message);
    }
}
