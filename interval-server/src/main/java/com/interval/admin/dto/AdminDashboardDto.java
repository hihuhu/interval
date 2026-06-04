package com.interval.admin.dto;

public record AdminDashboardDto(
    long totalUsers,
    long activeUsers,
    long disabledUsers,
    long activeToday,
    long activeLast7Days
) {
}
