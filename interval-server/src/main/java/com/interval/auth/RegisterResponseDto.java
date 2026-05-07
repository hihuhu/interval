package com.interval.auth;

/**
 * 注册响应 DTO
 * 
 * @param userId 用户 ID
 * @param username 用户名
 */
public record RegisterResponseDto(Long userId, String username) {
}
