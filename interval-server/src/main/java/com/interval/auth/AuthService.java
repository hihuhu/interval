package com.interval.auth;

/**
 * 认证服务接口
 * 
 * 定义用户认证相关的业务逻辑
 */
public interface AuthService {
    
    /**
     * 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录响应（包含 JWT token）
     * @throws IllegalArgumentException 如果用户名或密码错误
     */
    LoginResponseDto login(String username, String password);
    
    /**
     * 用户注册
     * 
     * @param username 用户名
     * @param password 密码
     * @return 注册响应（包含用户信息）
     * @throws IllegalArgumentException 如果用户名已存在或密码不符合要求
     */
    RegisterResponseDto register(String username, String password);
}
