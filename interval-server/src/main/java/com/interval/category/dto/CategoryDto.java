package com.interval.category.dto;

/**
 * 分类 DTO
 */
public record CategoryDto(
    Long id,
    String name,
    String colorCode,
    String status,
    Integer displayOrder
) {
    public CategoryDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (colorCode == null || !colorCode.matches("^#[0-9a-fA-F]{6}$")) {
            throw new IllegalArgumentException("colorCode must be a valid hex color");
        }
    }
}
