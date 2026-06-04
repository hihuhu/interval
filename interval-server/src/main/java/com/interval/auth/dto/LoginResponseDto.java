package com.interval.auth.dto;

public record LoginResponseDto(
    String token,
    String username,
    String accountType,
    boolean mustChangePassword
) {
}
