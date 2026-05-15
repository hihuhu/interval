package com.interval.category.service;

import com.interval.category.dto.CategoryDto;
import com.interval.category.dto.DeleteCategoryResponseDto;
import com.interval.category.entity.Category;
import com.interval.category.entity.CategoryStatus;
import com.interval.category.repository.CategoryRepository;
import com.interval.timeslot.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 分类服务实现
 */
@Service
public class CategoryServiceImpl implements CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    
    @Override
    public List<CategoryDto> getActiveCategories(Long userId) {
        return categoryRepository.findActiveByUserId(userId)
            .stream()
            .map(this::toDto)
            .toList();
    }
    
    @Override
    @Transactional
    public CategoryDto createCategory(Long userId, String name, String colorCode) {
        // 检查活跃分类名称是否已存在
        if (categoryRepository.existsActiveByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("Active category with this name already exists");
        }
        
        // 获取当前最大的 displayOrder
        Integer maxDisplayOrder = categoryRepository.findMaxDisplayOrderByUserId(userId);
        
        // 创建新分类
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setColorCode(colorCode);
        category.setStatus(CategoryStatus.ACTIVE);
        category.setDisplayOrder(maxDisplayOrder + 1);
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());
        
        Category saved = categoryRepository.save(category);
        return toDto(saved);
    }
    
    @Override
    @Transactional
    public CategoryDto updateCategory(Long userId, Long categoryId, String name, String colorCode, Integer displayOrder) {
        // 验证分类存在且属于当前用户
        Category category = categoryRepository.findByUserIdAndId(userId, categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        
        // 如果修改了名称，检查新名称是否与其他活跃分类重复
        if (!category.getName().equals(name)) {
            if (categoryRepository.existsActiveByUserIdAndName(userId, name)) {
                throw new IllegalArgumentException("Active category with this name already exists");
            }
        }
        
        // 更新分类
        category.setName(name);
        category.setColorCode(colorCode);
        if (displayOrder != null) {
            category.setDisplayOrder(displayOrder);
        }
        category.setUpdatedAt(Instant.now());
        
        Category updated = categoryRepository.save(category);
        return toDto(updated);
    }
    
    @Override
    @Transactional
    public DeleteCategoryResponseDto deleteCategory(Long userId, Long categoryId) {
        // 1. 验证分类存在且属于当前用户
        Category category = categoryRepository.findByUserIdAndId(userId, categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        
        // 2. 检查是否有历史记录使用该分类
        long recordCount = timeSlotRepository.countByCategoryId(categoryId);
        
        if (recordCount == 0) {
            // 场景 A：无记录，物理删除
            categoryRepository.delete(category);
            return new DeleteCategoryResponseDto("DELETED", 0L);
        } else {
            // 场景 B：有记录，归档
            category.setStatus(CategoryStatus.ARCHIVED);
            category.setUpdatedAt(Instant.now());
            categoryRepository.save(category);
            
            return new DeleteCategoryResponseDto("ARCHIVED", recordCount);
        }
    }
    
    /**
     * 将 Category 实体转换为 DTO
     */
    private CategoryDto toDto(Category category) {
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getColorCode(),
            category.getStatus().name(),
            category.getDisplayOrder()
        );
    }
}
