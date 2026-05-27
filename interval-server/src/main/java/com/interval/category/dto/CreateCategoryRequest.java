package com.interval.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * CreateCategoryRequest
 * 
 * 创建分类的请求 DTO。
 * 
 * @param name 分类名称（1-50 字符）
 * @param colorCode 颜色代码（十六进制格式，如 #3b82f6）
 */
public record CreateCategoryRequest(
    @NotBlank(message = "Category name is required")
    @Size(min = 1, max = 50, message = "Category name must be between 1 and 50 characters")
    String name,
    
    @NotBlank(message = "Color code is required")
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color code must be in hex format (e.g., #3b82f6)")
    String colorCode
) {
    /**
     * Compact canonical constructor for validation
     */
    public CreateCategoryRequest {
        if (name != null) {
            name = name.trim();
        }
        if (colorCode != null) {
            colorCode = colorCode.toLowerCase();
        }
    }
}
