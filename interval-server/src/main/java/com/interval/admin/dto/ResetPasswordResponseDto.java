package com.interval.admin.dto;

public record ResetPasswordResponseDto(String temporaryPassword, boolean mustChangePassword) {
}
