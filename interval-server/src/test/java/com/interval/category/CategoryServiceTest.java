package com.interval.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CategoryService 测试类
 * 
 * 测试分类管理模块的核心业务逻辑，包括：
 * - 获取用户分类列表
 * - 创建新分类
 * - 更新分类
 * - 删除分类（包括使用中的分类检查）
 * 
 * 测试策略：
 * - 使用 Mockito 模拟 CategoryRepository 和 TimeSlotRepository
 * - 验证 Service 层的业务逻辑和数据隔离
 * - 覆盖正常流程和异常场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("分类服务测试")
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Long testUserId = 1L;
    private Category workCategory;
    private Category studyCategory;
    private Category restCategory;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        workCategory = new Category();
        workCategory.setId(1L);
        workCategory.setUserId(testUserId);
        workCategory.setName("工作");
        workCategory.setColorCode("#3b82f6");
        workCategory.setDisplayOrder(0);

        studyCategory = new Category();
        studyCategory.setId(2L);
        studyCategory.setUserId(testUserId);
        studyCategory.setName("学习");
        studyCategory.setColorCode("#8b5cf6");
        studyCategory.setDisplayOrder(1);

        restCategory = new Category();
        restCategory.setId(3L);
        restCategory.setUserId(testUserId);
        restCategory.setName("休息");
        restCategory.setColorCode("#22c55e");
        restCategory.setDisplayOrder(2);
    }

    /**
     * 测试用例 1：获取用户分类列表
     * 
     * Given: 用户 alex (userId=1) 有 3 个分类
     * When: 调用 CategoryService.getUserCategories(1)
     * Then:
     *   - 返回包含 3 个 CategoryDto 的列表
     *   - 按 displayOrder 升序排列
     */
    @Test
    @DisplayName("获取用户分类列表应返回按顺序排列的分类")
    void should_return_ordered_categories_when_get_user_categories() {
        // Given: 用户有 3 个分类
        List<Category> categories = Arrays.asList(workCategory, studyCategory, restCategory);
        when(categoryRepository.findByUserIdOrderByDisplayOrderAsc(testUserId))
            .thenReturn(categories);

        // When: 获取用户分类列表
        List<CategoryDto> result = categoryService.getUserCategories(testUserId);

        // Then: 验证返回结果
        assertNotNull(result, "分类列表不应为空");
        assertEquals(3, result.size(), "应返回 3 个分类");
        
        // 验证顺序
        assertEquals("工作", result.get(0).name(), "第一个分类应为'工作'");
        assertEquals("学习", result.get(1).name(), "第二个分类应为'学习'");
        assertEquals("休息", result.get(2).name(), "第三个分类应为'休息'");
        
        // 验证 displayOrder
        assertEquals(0, result.get(0).displayOrder(), "第一个分类的 displayOrder 应为 0");
        assertEquals(1, result.get(1).displayOrder(), "第二个分类的 displayOrder 应为 1");
        assertEquals(2, result.get(2).displayOrder(), "第三个分类的 displayOrder 应为 2");

        // 验证方法调用
        verify(categoryRepository, times(1)).findByUserIdOrderByDisplayOrderAsc(testUserId);
    }

    /**
     * 测试用例 2：创建新分类成功
     * 
     * Given: 用户 alex (userId=1) 没有名为"运动"的分类
     * When: 调用 CategoryService.createCategory(1, "运动", "#f97316")
     * Then:
     *   - 返回新创建的 CategoryDto
     *   - id 字段非空
     *   - displayOrder 自动设置为当前最大值 + 1
     */
    @Test
    @DisplayName("创建新分类应成功并自动设置 displayOrder")
    void should_create_category_with_auto_display_order() {
        // Given: 用户没有名为"运动"的分类
        String newCategoryName = "运动";
        String newColorCode = "#f97316";
        
        when(categoryRepository.existsByUserIdAndName(testUserId, newCategoryName))
            .thenReturn(false);
        when(categoryRepository.findMaxDisplayOrderByUserId(testUserId))
            .thenReturn(Optional.of(2)); // 当前最大 displayOrder 为 2
        
        Category savedCategory = new Category();
        savedCategory.setId(4L);
        savedCategory.setUserId(testUserId);
        savedCategory.setName(newCategoryName);
        savedCategory.setColorCode(newColorCode);
        savedCategory.setDisplayOrder(3);
        
        when(categoryRepository.save(any(Category.class)))
            .thenReturn(savedCategory);

        // When: 创建新分类
        CategoryDto result = categoryService.createCategory(testUserId, newCategoryName, newColorCode);

        // Then: 验证返回结果
        assertNotNull(result, "创建的分类不应为空");
        assertEquals(4L, result.id(), "分类 ID 应正确");
        assertEquals(newCategoryName, result.name(), "分类名称应正确");
        assertEquals(newColorCode, result.colorCode(), "颜色代码应正确");
        assertEquals(3, result.displayOrder(), "displayOrder 应为当前最大值 + 1");

        // 验证方法调用
        verify(categoryRepository, times(1)).existsByUserIdAndName(testUserId, newCategoryName);
        verify(categoryRepository, times(1)).findMaxDisplayOrderByUserId(testUserId);
        verify(categoryRepository, times(1)).save(argThat(category ->
            category.getUserId().equals(testUserId) &&
            category.getName().equals(newCategoryName) &&
            category.getColorCode().equals(newColorCode) &&
            category.getDisplayOrder() == 3
        ));
    }

    /**
     * 测试用例 3：创建分类时名称重复
     * 
     * Given: 用户 alex (userId=1) 已有名为"工作"的分类
     * When: 调用 CategoryService.createCategory(1, "工作", "#3b82f6")
     * Then:
     *   - 抛出 IllegalArgumentException
     *   - 异常消息为 "Category name already exists"
     */
    @Test
    @DisplayName("创建重复名称的分类应抛出异常")
    void should_throw_exception_when_category_name_already_exists() {
        // Given: 分类名称已存在
        String existingName = "工作";
        String colorCode = "#3b82f6";
        
        when(categoryRepository.existsByUserIdAndName(testUserId, existingName))
            .thenReturn(true);

        // When & Then: 创建分类应抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.createCategory(testUserId, existingName, colorCode),
            "分类名称已存在时应抛出 IllegalArgumentException"
        );

        assertEquals("Category name already exists", exception.getMessage(),
            "异常消息应为 'Category name already exists'");

        // 验证方法调用
        verify(categoryRepository, times(1)).existsByUserIdAndName(testUserId, existingName);
        verify(categoryRepository, never()).save(any());
    }

    /**
     * 测试用例 4：删除未被使用的分类
     * 
     * Given: 用户 alex (userId=1) 有分类 id=5
     *        没有任何 TimeSlot 引用该分类
     * When: 调用 CategoryService.deleteCategory(1, 5)
     * Then:
     *   - 分类被成功删除
     *   - 不抛出异常
     */
    @Test
    @DisplayName("删除未被使用的分类应成功")
    void should_delete_category_when_not_in_use() {
        // Given: 分类存在且未被使用
        Long categoryId = 5L;
        Category unusedCategory = new Category();
        unusedCategory.setId(categoryId);
        unusedCategory.setUserId(testUserId);
        unusedCategory.setName("未使用分类");
        
        when(categoryRepository.findByIdAndUserId(categoryId, testUserId))
            .thenReturn(Optional.of(unusedCategory));
        when(timeSlotRepository.existsByCategoryId(categoryId))
            .thenReturn(false);

        // When: 删除分类
        assertDoesNotThrow(() -> categoryService.deleteCategory(testUserId, categoryId),
            "删除未被使用的分类不应抛出异常");

        // Then: 验证方法调用
        verify(categoryRepository, times(1)).findByIdAndUserId(categoryId, testUserId);
        verify(timeSlotRepository, times(1)).existsByCategoryId(categoryId);
        verify(categoryRepository, times(1)).delete(unusedCategory);
    }

    /**
     * 测试用例 5：删除正在使用的分类
     * 
     * Given: 用户 alex (userId=1) 有分类 id=1
     *        存在 TimeSlot 记录的 category_id=1
     * When: 调用 CategoryService.deleteCategory(1, 1)
     * Then:
     *   - 抛出 IllegalArgumentException
     *   - 异常消息为 "Cannot delete category: it is being used by time slots"
     */
    @Test
    @DisplayName("删除正在使用的分类应抛出异常")
    void should_throw_exception_when_delete_category_in_use() {
        // Given: 分类正在被使用
        Long categoryId = 1L;
        
        when(categoryRepository.findByIdAndUserId(categoryId, testUserId))
            .thenReturn(Optional.of(workCategory));
        when(timeSlotRepository.existsByCategoryId(categoryId))
            .thenReturn(true);

        // When & Then: 删除分类应抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.deleteCategory(testUserId, categoryId),
            "删除正在使用的分类时应抛出 IllegalArgumentException"
        );

        assertEquals("Cannot delete category: it is being used by time slots", 
            exception.getMessage(),
            "异常消息应为 'Cannot delete category: it is being used by time slots'");

        // 验证方法调用
        verify(categoryRepository, times(1)).findByIdAndUserId(categoryId, testUserId);
        verify(timeSlotRepository, times(1)).existsByCategoryId(categoryId);
        verify(categoryRepository, never()).delete(any());
    }

    /**
     * 测试用例 6：更新分类信息
     * 
     * Given: 用户 alex (userId=1) 有分类 id=1
     * When: 调用 CategoryService.updateCategory(1, 1, "工作时间", "#2563eb", 0)
     * Then:
     *   - 返回更新后的 CategoryDto
     *   - 分类信息已更新
     */
    @Test
    @DisplayName("更新分类信息应成功")
    void should_update_category_successfully() {
        // Given: 分类存在
        Long categoryId = 1L;
        String newName = "工作时间";
        String newColorCode = "#2563eb";
        Integer newDisplayOrder = 0;
        
        when(categoryRepository.findByIdAndUserId(categoryId, testUserId))
            .thenReturn(Optional.of(workCategory));
        when(categoryRepository.existsByUserIdAndNameAndIdNot(testUserId, newName, categoryId))
            .thenReturn(false);
        
        Category updatedCategory = new Category();
        updatedCategory.setId(categoryId);
        updatedCategory.setUserId(testUserId);
        updatedCategory.setName(newName);
        updatedCategory.setColorCode(newColorCode);
        updatedCategory.setDisplayOrder(newDisplayOrder);
        
        when(categoryRepository.save(any(Category.class)))
            .thenReturn(updatedCategory);

        // When: 更新分类
        CategoryDto result = categoryService.updateCategory(
            testUserId, categoryId, newName, newColorCode, newDisplayOrder
        );

        // Then: 验证返回结果
        assertNotNull(result, "更新后的分类不应为空");
        assertEquals(categoryId, result.id(), "分类 ID 应保持不变");
        assertEquals(newName, result.name(), "分类名称应已更新");
        assertEquals(newColorCode, result.colorCode(), "颜色代码应已更新");
        assertEquals(newDisplayOrder, result.displayOrder(), "显示顺序应已更新");

        // 验证方法调用
        verify(categoryRepository, times(1)).findByIdAndUserId(categoryId, testUserId);
        verify(categoryRepository, times(1)).save(workCategory);
    }

    /**
     * 测试用例 7：数据隔离 - 不能访问其他用户的分类
     * 
     * Given: 分类 id=1 属于用户 1
     * When: 用户 2 尝试删除分类 id=1
     * Then:
     *   - 抛出 IllegalArgumentException
     *   - 异常消息为 "Category not found"
     */
    @Test
    @DisplayName("不能操作其他用户的分类")
    void should_not_access_other_users_categories() {
        // Given: 分类属于用户 1
        Long categoryId = 1L;
        Long otherUserId = 2L;
        
        when(categoryRepository.findByIdAndUserId(categoryId, otherUserId))
            .thenReturn(Optional.empty());

        // When & Then: 用户 2 尝试删除应抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.deleteCategory(otherUserId, categoryId),
            "访问其他用户的分类时应抛出 IllegalArgumentException"
        );

        assertEquals("Category not found", exception.getMessage(),
            "异常消息应为 'Category not found'");

        // 验证不应执行删除操作
        verify(categoryRepository, never()).delete(any());
    }
}
