package com.interval.category;

import com.interval.category.dto.CategoryDto;
import com.interval.category.dto.DeleteCategoryResponseDto;
import com.interval.category.entity.Category;
import com.interval.category.entity.CategoryStatus;
import com.interval.category.repository.CategoryRepository;
import com.interval.category.service.CategoryService;
import com.interval.timeslot.entity.TimeSlot;
import com.interval.timeslot.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 分类智能删除与归档功能测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategorySmartDeleteTest {
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    
    private Long testUserId;
    
    @BeforeEach
    void setUp() {
        testUserId = 1L;
        
        // 清理测试数据
        timeSlotRepository.deleteAll();
        categoryRepository.deleteAll();
    }
    
    @Test
    @DisplayName("测试 1：场景 A - 分类无历史记录时，应物理删除")
    void should_delete_category_when_no_records_exist() {
        // Given: 用户创建分类"测试分类"
        CategoryDto category = categoryService.createCategory(testUserId, "测试分类", "#3b82f6");
        Long categoryId = category.id();
        
        // When: 用户删除该分类
        DeleteCategoryResponseDto result = categoryService.deleteCategory(testUserId, categoryId);
        
        // Then: 返回 DELETED 动作
        assertThat(result.action()).isEqualTo("DELETED");
        assertThat(result.affectedRecords()).isEqualTo(0L);
        
        // Then: 数据库中该分类已被物理删除
        assertThat(categoryRepository.findById(categoryId)).isEmpty();
    }
    
    @Test
    @DisplayName("测试 2：场景 B - 分类有历史记录时，应自动归档而非删除")
    void should_archive_category_when_records_exist() {
        // Given: 用户创建分类"工作"
        CategoryDto workCategory = categoryService.createCategory(testUserId, "工作", "#3b82f6");
        
        // Given: 用户记录了 3 条使用"工作"分类的时间块
        Category category = categoryRepository.findById(workCategory.id()).orElseThrow();
        for (int i = 0; i < 3; i++) {
            TimeSlot slot = new TimeSlot();
            slot.setUserId(testUserId);
            slot.setDate(LocalDate.of(2026, 5, 8));
            slot.setSlotIndex(i);
            slot.setActivityName("工作任务 " + i);
            slot.setCategory(category);
            slot.setCreatedAt(Instant.now());
            slot.setUpdatedAt(Instant.now());
            timeSlotRepository.save(slot);
        }
        
        // When: 用户尝试删除"工作"分类
        DeleteCategoryResponseDto result = categoryService.deleteCategory(testUserId, workCategory.id());
        
        // Then: 返回 ARCHIVED 动作
        assertThat(result.action()).isEqualTo("ARCHIVED");
        assertThat(result.affectedRecords()).isEqualTo(3L);
        
        // Then: 数据库中该分类状态变为 ARCHIVED
        Category archived = categoryRepository.findById(workCategory.id()).orElseThrow();
        assertThat(archived.getStatus()).isEqualTo(CategoryStatus.ARCHIVED);
    }
    
    @Test
    @DisplayName("测试 3：归档后的分类不应出现在活跃分类列表中")
    void archived_category_should_not_appear_in_active_list() {
        // Given: 用户有 3 个分类
        CategoryDto cat1 = categoryService.createCategory(testUserId, "工作", "#3b82f6");
        CategoryDto cat2 = categoryService.createCategory(testUserId, "学习", "#8b5cf6");
        CategoryDto cat3 = categoryService.createCategory(testUserId, "运动", "#f97316");
        
        // Given: "学习"分类有历史记录
        Category category = categoryRepository.findById(cat2.id()).orElseThrow();
        TimeSlot slot = new TimeSlot();
        slot.setUserId(testUserId);
        slot.setDate(LocalDate.now());
        slot.setSlotIndex(0);
        slot.setActivityName("看书");
        slot.setCategory(category);
        slot.setCreatedAt(Instant.now());
        slot.setUpdatedAt(Instant.now());
        timeSlotRepository.save(slot);
        
        // When: 归档"学习"分类
        categoryService.deleteCategory(testUserId, cat2.id());
        
        // Then: 活跃分类列表只包含"工作"和"运动"
        List<CategoryDto> activeCategories = categoryService.getActiveCategories(testUserId);
        assertThat(activeCategories).hasSize(2);
        assertThat(activeCategories)
            .extracting(CategoryDto::name)
            .containsExactlyInAnyOrder("工作", "运动");
    }
    
    @Test
    @DisplayName("测试 4：归档分类后，用户应该可以重新创建同名的活跃分类")
    void should_allow_creating_active_category_with_same_name_after_archiving() {
        // Given: 用户创建并归档"运动"分类
        CategoryDto oldCategory = categoryService.createCategory(testUserId, "运动", "#f97316");
        
        Category category = categoryRepository.findById(oldCategory.id()).orElseThrow();
        TimeSlot slot = new TimeSlot();
        slot.setUserId(testUserId);
        slot.setDate(LocalDate.now());
        slot.setSlotIndex(0);
        slot.setActivityName("跑步");
        slot.setCategory(category);
        slot.setCreatedAt(Instant.now());
        slot.setUpdatedAt(Instant.now());
        timeSlotRepository.save(slot);
        
        categoryService.deleteCategory(testUserId, oldCategory.id());
        
        // When: 用户重新创建同名分类（但颜色不同）
        CategoryDto newCategory = categoryService.createCategory(testUserId, "运动", "#ef4444");
        
        // Then: 创建成功，且 ID 不同
        assertThat(newCategory.id()).isNotEqualTo(oldCategory.id());
        assertThat(newCategory.name()).isEqualTo("运动");
        assertThat(newCategory.status()).isEqualTo("ACTIVE");
        assertThat(newCategory.colorCode()).isEqualTo("#ef4444");
        
        // Then: 活跃分类列表中只显示新分类
        List<CategoryDto> activeCategories = categoryService.getActiveCategories(testUserId);
        assertThat(activeCategories)
            .filteredOn(c -> c.name().equals("运动"))
            .hasSize(1)
            .first()
            .extracting(CategoryDto::id)
            .isEqualTo(newCategory.id());
    }
    
    @Test
    @DisplayName("测试 5：创建分类时，应仅检查活跃分类的名称唯一性")
    void should_check_only_active_category_name_uniqueness() {
        // Given: 用户创建并归档"工作"分类
        CategoryDto oldCategory = categoryService.createCategory(testUserId, "工作", "#3b82f6");
        
        Category category = categoryRepository.findById(oldCategory.id()).orElseThrow();
        TimeSlot slot = new TimeSlot();
        slot.setUserId(testUserId);
        slot.setDate(LocalDate.now());
        slot.setSlotIndex(0);
        slot.setActivityName("写代码");
        slot.setCategory(category);
        slot.setCreatedAt(Instant.now());
        slot.setUpdatedAt(Instant.now());
        timeSlotRepository.save(slot);
        
        categoryService.deleteCategory(testUserId, oldCategory.id());
        
        // When: 用户尝试创建同名的活跃分类
        CategoryDto newCategory = categoryService.createCategory(testUserId, "工作", "#2563eb");
        
        // Then: 创建成功（因为旧的"工作"已归档）
        assertThat(newCategory).isNotNull();
        assertThat(newCategory.status()).isEqualTo("ACTIVE");
        
        // When: 用户再次尝试创建同名分类
        // Then: 应抛出异常（因为已有活跃的"工作"分类）
        assertThatThrownBy(() -> categoryService.createCategory(testUserId, "工作", "#1e40af"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Active category with this name already exists");
    }
    
    @Test
    @DisplayName("测试 6：用户 A 无法删除用户 B 的分类")
    void should_prevent_cross_user_category_deletion() {
        // Given: 用户 A 创建分类
        Long userA = 1L;
        CategoryDto categoryA = categoryService.createCategory(userA, "工作", "#3b82f6");
        
        // When: 用户 B 尝试删除用户 A 的分类
        Long userB = 2L;
        
        // Then: 应抛出异常
        assertThatThrownBy(() -> categoryService.deleteCategory(userB, categoryA.id()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Category not found");
    }
}
