package com.interval.stats.dto;

import com.interval.category.entity.CategoryStatus;

public record CategoryDurationStatRawDto(
    Long categoryId,
    String categoryName,
    String categoryColor,
    CategoryStatus categoryStatus,
    Integer displayOrder,
    Long slotCount
) {
}
