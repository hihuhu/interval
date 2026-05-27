package com.interval.category.service;

import com.interval.category.dto.CategoryDto;
import com.interval.category.dto.DeleteCategoryResponseDto;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {
    
    /**
     * 获取用户的所有活跃分类
     */
    List<CategoryDto> getActiveCategories(Long userId);
    
    /**
     * 创建新分类
     */
    CategoryDto createCategory(Long userId, String name, String colorCode);
    
    /**
     * 更新分类
     */
    CategoryDto updateCategory(Long userId, Long categoryId, String name, String colorCode, Integer displayOrder);
    
    /**
     * 智能删除分类
     * - 无历史记录：物理删除
     * - 有历史记录：归档
     */
    DeleteCategoryResponseDto deleteCategory(Long userId, Long categoryId);
}
