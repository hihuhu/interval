package com.interval.admin.dto;

import java.time.Instant;

public record AdminUserDetailDto(
    Long userId,
    String username,
    String status,
    Instant createdAt,
    Instant lastLoginAt,
    Instant lastActiveAt,
    boolean mustChangePassword
) {
}
