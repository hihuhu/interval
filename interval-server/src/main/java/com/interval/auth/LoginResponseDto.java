package com.interval.auth;

/**
 * 登录响应 DTO
 * 
 * @param token JWT token
 * @param username 用户名
 */
public record LoginResponseDto(String token, String username) {
}
