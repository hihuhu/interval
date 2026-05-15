package com.interval.category.controller;

import com.interval.category.dto.CategoryDto;
import com.interval.category.dto.CreateCategoryRequest;
import com.interval.category.dto.UpdateCategoryRequest;
import com.interval.category.service.CategoryService;
import com.interval.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CategoryController
 * 
 * 分类管理 REST API 控制器。
 * 提供分类的 CRUD 操作接口。
 * 
 * 所有接口都需要认证，userId 从 JWT token 中提取。
 * 目前为了测试方便，暂时使用 @RequestHeader 接收 userId。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    
    private final CategoryService categoryService;
    
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    
    /**
     * 获取当前用户的所有分类
     * 
     * @param userId 用户 ID（从 JWT token 中提取，暂时用 header）
     * @return 分类列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getUserCategories(
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<CategoryDto> categories = categoryService.getActiveCategories(userId);
            return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("ERROR", e.getMessage(), null));
        }
    }
    
    /**
     * 创建新分类
     * 
     * @param userId 用户 ID（从 JWT token 中提取，暂时用 header）
     * @param request 创建分类请求
     * @return 创建的分类
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateCategoryRequest request) {
        try {
            CategoryDto category = categoryService.createCategory(
                    userId, 
                    request.name(), 
                    request.colorCode()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Category created successfully", category));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("ERROR", e.getMessage(), null));
        }
    }
    
    /**
     * 更新分类
     * 
     * @param userId 用户 ID（从 JWT token 中提取，暂时用 header）
     * @param categoryId 分类 ID
     * @param request 更新分类请求
     * @return 更新后的分类
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        try {
            CategoryDto category = categoryService.updateCategory(
                    userId,
                    categoryId,
                    request.name(),
                    request.colorCode(),
                    request.displayOrder()
            );
            return ResponseEntity.ok(ApiResponse.success("Category updated successfully", category));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("ERROR", e.getMessage(), null));
        }
    }
    
    /**
     * 删除分类
     * 
     * @param userId 用户 ID（从 JWT token 中提取，暂时用 header）
     * @param categoryId 分类 ID
     * @return 删除结果
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long categoryId) {
        try {
            categoryService.deleteCategory(userId, categoryId);
            return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("ERROR", e.getMessage(), null));
        }
    }
}
