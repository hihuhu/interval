package com.interval.stats.dto;

public record CategoryDurationStatDto(
    Long categoryId,
    String categoryName,
    String categoryColor,
    String categoryStatus,
    Long slotCount,
    Long durationMinutes,
    Double percentage
) {
}
