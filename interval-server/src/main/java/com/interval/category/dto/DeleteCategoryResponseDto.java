package com.interval.category.dto;

/**
 * 删除分类响应 DTO
 */
public record DeleteCategoryResponseDto(
    String action,        // "DELETED" | "ARCHIVED"
    Long affectedRecords  // 受影响的历史记录数量
) {
    public DeleteCategoryResponseDto {
        if (action == null || (!action.equals("DELETED") && !action.equals("ARCHIVED"))) {
            throw new IllegalArgumentException("action must be DELETED or ARCHIVED");
        }
        if (affectedRecords == null || affectedRecords < 0) {
            throw new IllegalArgumentException("affectedRecords must be non-negative");
        }
    }
}
