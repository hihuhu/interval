package com.interval.category;

import com.interval.category.dto.CategoryDto;
import com.interval.category.dto.DeleteCategoryResponseDto;
import com.interval.category.entity.Category;
import com.interval.category.entity.CategoryStatus;
import com.interval.category.repository.CategoryRepository;
import com.interval.category.service.CategoryServiceImpl;
import com.interval.timeslot.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("分类服务测试")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private final Long testUserId = 1L;
    private Category workCategory;
    private Category studyCategory;
    private Category restCategory;

    @BeforeEach
    void setUp() {
        workCategory = createCategory(1L, testUserId, "工作", "#3b82f6", CategoryStatus.ACTIVE, 0);
        studyCategory = createCategory(2L, testUserId, "学习", "#8b5cf6", CategoryStatus.ACTIVE, 1);
        restCategory = createCategory(3L, testUserId, "休息", "#10b981", CategoryStatus.ACTIVE, 2);
    }

    @Test
    @DisplayName("获取活跃分类列表应返回按 displayOrder 排序的结果")
    void should_return_active_categories_ordered_by_display_order() {
        List<Category> categories = List.of(workCategory, studyCategory, restCategory);
        when(categoryRepository.findActiveByUserId(testUserId)).thenReturn(categories);

        List<CategoryDto> result = categoryService.getActiveCategories(testUserId);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("工作", result.get(0).name());
        assertEquals("学习", result.get(1).name());
        assertEquals("休息", result.get(2).name());
        assertEquals("ACTIVE", result.get(0).status());
        verify(categoryRepository).findActiveByUserId(testUserId);
    }

    @Test
    @DisplayName("创建新分类应成功并自动设置 displayOrder")
    void should_create_category_with_auto_display_order() {
        String newCategoryName = "运动";
        String newColorCode = "#f97316";
        Category savedCategory = createCategory(4L, testUserId, newCategoryName, newColorCode, CategoryStatus.ACTIVE, 3);

        when(categoryRepository.existsActiveByUserIdAndName(testUserId, newCategoryName)).thenReturn(false);
        when(categoryRepository.findMaxDisplayOrderByUserId(testUserId)).thenReturn(2);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryDto result = categoryService.createCategory(testUserId, newCategoryName, newColorCode);

        assertNotNull(result);
        assertEquals(4L, result.id());
        assertEquals(newCategoryName, result.name());
        assertEquals(newColorCode, result.colorCode());
        assertEquals("ACTIVE", result.status());
        assertEquals(3, result.displayOrder());
        verify(categoryRepository).existsActiveByUserIdAndName(testUserId, newCategoryName);
        verify(categoryRepository).findMaxDisplayOrderByUserId(testUserId);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("创建重复活跃名称的分类应抛出异常")
    void should_throw_exception_when_active_category_name_already_exists() {
        String existingName = "工作";
        when(categoryRepository.existsActiveByUserIdAndName(testUserId, existingName)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.createCategory(testUserId, existingName, "#3b82f6")
        );

        assertEquals("Active category with this name already exists", exception.getMessage());
        verify(categoryRepository).existsActiveByUserIdAndName(testUserId, existingName);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("删除未被使用的分类应物理删除")
    void should_delete_category_when_not_in_use() {
        Long categoryId = 5L;
        Category unusedCategory = createCategory(categoryId, testUserId, "未使用分类", "#ef4444", CategoryStatus.ACTIVE, 3);

        when(categoryRepository.findByUserIdAndId(testUserId, categoryId)).thenReturn(Optional.of(unusedCategory));
        when(timeSlotRepository.countByCategoryId(categoryId)).thenReturn(0L);

        DeleteCategoryResponseDto result = categoryService.deleteCategory(testUserId, categoryId);

        assertEquals("DELETED", result.action());
        assertEquals(0L, result.affectedRecords());
        verify(categoryRepository).findByUserIdAndId(testUserId, categoryId);
        verify(timeSlotRepository).countByCategoryId(categoryId);
        verify(categoryRepository).delete(unusedCategory);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("删除正在使用的分类应归档")
    void should_archive_category_when_in_use() {
        Long categoryId = 1L;
        when(categoryRepository.findByUserIdAndId(testUserId, categoryId)).thenReturn(Optional.of(workCategory));
        when(timeSlotRepository.countByCategoryId(categoryId)).thenReturn(2L);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeleteCategoryResponseDto result = categoryService.deleteCategory(testUserId, categoryId);

        assertEquals("ARCHIVED", result.action());
        assertEquals(2L, result.affectedRecords());
        assertEquals(CategoryStatus.ARCHIVED, workCategory.getStatus());
        verify(categoryRepository).findByUserIdAndId(testUserId, categoryId);
        verify(timeSlotRepository).countByCategoryId(categoryId);
        verify(categoryRepository).save(workCategory);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("更新分类应成功")
    void should_update_category_successfully() {
        Long categoryId = 1L;
        String newName = "工作时间";
        String newColorCode = "#2563eb";
        Integer newDisplayOrder = 0;
        Category updatedCategory = createCategory(categoryId, testUserId, newName, newColorCode, CategoryStatus.ACTIVE, newDisplayOrder);

        when(categoryRepository.findByUserIdAndId(testUserId, categoryId)).thenReturn(Optional.of(workCategory));
        when(categoryRepository.existsActiveByUserIdAndName(testUserId, newName)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        CategoryDto result = categoryService.updateCategory(
            testUserId, categoryId, newName, newColorCode, newDisplayOrder
        );

        assertNotNull(result);
        assertEquals(categoryId, result.id());
        assertEquals(newName, result.name());
        assertEquals(newColorCode, result.colorCode());
        assertEquals(newDisplayOrder, result.displayOrder());
        verify(categoryRepository).findByUserIdAndId(testUserId, categoryId);
        verify(categoryRepository).existsActiveByUserIdAndName(testUserId, newName);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("更新为重复活跃分类名称应抛出异常")
    void should_throw_exception_when_updating_to_duplicate_active_name() {
        Long categoryId = 1L;
        String duplicateName = "学习";

        when(categoryRepository.findByUserIdAndId(testUserId, categoryId)).thenReturn(Optional.of(workCategory));
        when(categoryRepository.existsActiveByUserIdAndName(testUserId, duplicateName)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.updateCategory(testUserId, categoryId, duplicateName, "#2563eb", 0)
        );

        assertEquals("Active category with this name already exists", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("用户不能操作其他用户的分类")
    void should_isolate_categories_by_user() {
        Long categoryId = 1L;
        Long otherUserId = 2L;
        when(categoryRepository.findByUserIdAndId(otherUserId, categoryId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> categoryService.deleteCategory(otherUserId, categoryId)
        );

        assertEquals("Category not found", exception.getMessage());
        verify(timeSlotRepository, never()).countByCategoryId(any());
        verify(categoryRepository, never()).delete(any());
    }

    private Category createCategory(
        Long id,
        Long userId,
        String name,
        String colorCode,
        CategoryStatus status,
        Integer displayOrder
    ) {
        Category category = new Category();
        category.setId(id);
        category.setUserId(userId);
        category.setName(name);
        category.setColorCode(colorCode);
        category.setStatus(status);
        category.setDisplayOrder(displayOrder);
        return category;
    }
}
