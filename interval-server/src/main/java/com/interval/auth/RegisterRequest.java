package com.interval.auth;

/**
 * 注册请求 DTO
 * 
 * @param username 用户名
 * @param password 密码
 */
public record RegisterRequest(String username, String password) {
    
    /**
     * 紧凑构造器 - 验证输入参数
     */
    public RegisterRequest {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }
}
