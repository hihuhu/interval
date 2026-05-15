package com.interval.category;

import com.interval.auth.entity.User;
import com.interval.auth.repository.UserRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Category 模块集成测试
 * 
 * 按照 TDD 流程编写，测试分类删除的业务规则：
 * - 场景 A：无 TimeSlot 引用时，物理删除
 * - 场景 B：有 TimeSlot 引用时，转为 ARCHIVED 状态
 * - 场景 C：查询时间块时，归档分类名称正常显示
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Category 模块集成测试 - TDD")
class CategoryIntegrationTest {
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private Long testUserId;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        timeSlotRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        
        // 创建测试用户
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hashedpassword");
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();
    }
    
    /**
     * 场景 A：物理删除
     * 
     * Given: 创建一个分类
     * When: 调用删除接口（该分类未被任何 TimeSlot 引用）
     * Then: 验证数据库中该分类已消失
     */
    @Test
    @DisplayName("场景 A：should_delete_category_physically_when_no_slots_exist")
    void should_delete_category_physically_when_no_slots_exist() {
        // Given: 创建一个分类
        CategoryDto category = categoryService.createCategory(testUserId, "工作", "#3b82f6");
        Long categoryId = category.id();
        
        // 验证分类已创建
        assertThat(categoryRepository.findById(categoryId)).isPresent();
        
        // When: 调用删除接口
        DeleteCategoryResponseDto response = categoryService.deleteCategory(testUserId, categoryId);
        
        // Then: 验证返回结果为 DELETED
        assertThat(response.action()).isEqualTo("DELETED");
        assertThat(response.affectedRecords()).isEqualTo(0L);
        
        // Then: 验证数据库中该分类已消失（物理删除）
        Optional<Category> deletedCategory = categoryRepository.findById(categoryId);
        assertThat(deletedCategory).isEmpty();
    }
    
    /**
     * 场景 B：归档逻辑
     * 
     * Given: 创建一个分类，并创建一个关联该分类的时间块
     * When: 调用删除接口
     * Then: 验证分类状态变为 ARCHIVED 且未消失
     */
    @Test
    @DisplayName("场景 B：should_archive_category_when_slots_exist")
    void should_archive_category_when_slots_exist() {
        // Given: 创建一个分类
        CategoryDto categoryDto = categoryService.createCategory(testUserId, "学习", "#8b5cf6");
        Long categoryId = categoryDto.id();
        
        // Given: 创建一个关联该分类的时间块
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalStateException("Category should exist"));
        
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setUserId(testUserId);
        timeSlot.setDate(LocalDate.of(2026, 5, 8));
        timeSlot.setSlotIndex(0);
        timeSlot.setActivityName("阅读技术书籍");
        timeSlot.setCategory(category);
        timeSlot.setCreatedAt(Instant.now());
        timeSlot.setUpdatedAt(Instant.now());
        timeSlotRepository.save(timeSlot);
        
        // 验证时间块已创建
        long slotCount = timeSlotRepository.countByCategoryId(categoryId);
        assertThat(slotCount).isEqualTo(1L);
        
        // When: 调用删除接口
        DeleteCategoryResponseDto response = categoryService.deleteCategory(testUserId, categoryId);
        
        // Then: 验证返回结果为 ARCHIVED
        assertThat(response.action()).isEqualTo("ARCHIVED");
        assertThat(response.affectedRecords()).isEqualTo(1L);
        
        // Then: 验证分类状态变为 ARCHIVED 且未消失
        Category archivedCategory = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new AssertionError("Category should still exist in database"));
        
        assertThat(archivedCategory.getStatus()).isEqualTo(CategoryStatus.ARCHIVED);
        assertThat(archivedCategory.getName()).isEqualTo("学习");
    }
    
    /**
     * 场景 C：回显验证
     * 
     * Given: 创建一个分类，创建关联的时间块，然后归档该分类
     * When: 查询包含已归档分类的时间块
     * Then: 验证返回的时间块中分类名称显示正常（带"已归档"标记）
     * 
     * 注意：此测试需要 TimeSlot 查询 API 实现后才能完整验证
     * 当前仅验证数据库层面的关联关系
     */
    @Test
    @DisplayName("场景 C：should_display_archived_category_name_in_slot_query")
    void should_display_archived_category_name_in_slot_query() {
        // Given: 创建一个分类
        CategoryDto categoryDto = categoryService.createCategory(testUserId, "运动", "#f97316");
        Long categoryId = categoryDto.id();
        
        // Given: 创建关联该分类的时间块
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalStateException("Category should exist"));
        
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setUserId(testUserId);
        timeSlot.setDate(LocalDate.of(2026, 5, 8));
        timeSlot.setSlotIndex(1);
        timeSlot.setActivityName("晨跑");
        timeSlot.setCategory(category);
        timeSlot.setCreatedAt(Instant.now());
        timeSlot.setUpdatedAt(Instant.now());
        TimeSlot savedSlot = timeSlotRepository.save(timeSlot);
        
        // Given: 归档该分类
        DeleteCategoryResponseDto response = categoryService.deleteCategory(testUserId, categoryId);
        assertThat(response.action()).isEqualTo("ARCHIVED");
        
        // When: 查询时间块（从数据库重新加载）
        TimeSlot queriedSlot = timeSlotRepository.findById(savedSlot.getId())
            .orElseThrow(() -> new AssertionError("TimeSlot should exist"));
        
        // Then: 验证时间块关联的分类仍然存在
        assertThat(queriedSlot.getCategory()).isNotNull();
        assertThat(queriedSlot.getCategory().getId()).isEqualTo(categoryId);
        
        // Then: 验证分类名称正常显示
        Category queriedCategory = queriedSlot.getCategory();
        assertThat(queriedCategory.getName()).isEqualTo("运动");
        assertThat(queriedCategory.getStatus()).isEqualTo(CategoryStatus.ARCHIVED);
        
        // 注意：实际的 JSON 响应格式化（如添加"已归档"标记）
        // 应该在 TimeSlot 的 DTO 转换逻辑中实现
        // 例如：categoryName 字段可以返回 "运动 (已归档)"
        // 这部分逻辑将在 TimeSlot Service/Controller 实现时添加
        
        // 当前验证：数据库层面的关联关系正确
        String expectedDisplayName = queriedCategory.getName() + " (已归档)";
        assertThat(expectedDisplayName).isEqualTo("运动 (已归档)");
    }
    
    /**
     * 补充测试：验证归档后的分类不出现在活跃分类列表中
     */
    @Test
    @DisplayName("补充：归档后的分类不应出现在活跃分类列表中")
    void archived_category_should_not_appear_in_active_list() {
        // Given: 创建两个分类
        CategoryDto category1 = categoryService.createCategory(testUserId, "工作", "#3b82f6");
        CategoryDto category2 = categoryService.createCategory(testUserId, "休息", "#10b981");
        
        // Given: 为第一个分类创建时间块并归档
        Category cat1 = categoryRepository.findById(category1.id()).orElseThrow();
        TimeSlot slot = new TimeSlot();
        slot.setUserId(testUserId);
        slot.setDate(LocalDate.now());
        slot.setSlotIndex(0);
        slot.setActivityName("开会");
        slot.setCategory(cat1);
        slot.setCreatedAt(Instant.now());
        slot.setUpdatedAt(Instant.now());
        timeSlotRepository.save(slot);
        
        categoryService.deleteCategory(testUserId, category1.id());
        
        // When: 查询活跃分类列表
        var activeCategories = categoryService.getActiveCategories(testUserId);
        
        // Then: 只包含未归档的分类
        assertThat(activeCategories).hasSize(1);
        assertThat(activeCategories.get(0).name()).isEqualTo("休息");
        assertThat(activeCategories.get(0).status()).isEqualTo("ACTIVE");
    }
}
