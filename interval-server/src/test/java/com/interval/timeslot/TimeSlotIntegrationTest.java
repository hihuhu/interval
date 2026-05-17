package com.interval.timeslot;

import com.interval.category.dto.CategoryDto;
import com.interval.category.entity.Category;
import com.interval.category.entity.CategoryStatus;
import com.interval.category.repository.CategoryRepository;
import com.interval.category.service.CategoryService;
import com.interval.timeslot.dto.DeleteTimeSlotResponseDto;
import com.interval.timeslot.dto.TimeSlotDto;
import com.interval.timeslot.dto.UpsertTimeSlotRequest;
import com.interval.timeslot.repository.TimeSlotRepository;
import com.interval.timeslot.service.TimeSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TimeSlot 集成测试")
class TimeSlotIntegrationTest {

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private final Long userId = 1L;
    private final Long otherUserId = 2L;
    private final LocalDate date = LocalDate.of(2026, 5, 16);
    private CategoryDto workCategory;

    @BeforeEach
    void setUp() {
        timeSlotRepository.deleteAll();
        categoryRepository.deleteAll();
        workCategory = categoryService.createCategory(userId, "工作", "#3b82f6");
    }

    @Test
    @DisplayName("创建并查询当前用户某天的时间格")
    void should_create_and_query_daily_slot() {
        UpsertTimeSlotRequest request = new UpsertTimeSlotRequest(date, 36, "写代码", workCategory.id());

        TimeSlotDto saved = timeSlotService.upsertTimeSlot(userId, request);
        List<TimeSlotDto> slots = timeSlotService.getDailySlots(userId, date);

        assertThat(saved.id()).isNotNull();
        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).slotIndex()).isEqualTo(36);
        assertThat(slots.get(0).activityName()).isEqualTo("写代码");
        assertThat(slots.get(0).categoryName()).isEqualTo("工作");
    }

    @Test
    @DisplayName("同一天同一格重复保存应覆盖而不是新增")
    void should_overwrite_existing_slot_for_same_user_date_and_index() {
        timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date, 36, "写代码", workCategory.id()));
        TimeSlotDto updated = timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date, 36, "改 Bug", workCategory.id()));

        List<TimeSlotDto> slots = timeSlotService.getDailySlots(userId, date);

        assertThat(slots).hasSize(1);
        assertThat(updated.activityName()).isEqualTo("改 Bug");
        assertThat(slots.get(0).activityName()).isEqualTo("改 Bug");
    }

    @Test
    @DisplayName("不同用户可以在同一天同一格独立记录")
    void should_isolate_slots_between_users() {
        CategoryDto otherCategory = categoryService.createCategory(otherUserId, "学习", "#8b5cf6");

        timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date, 36, "写代码", workCategory.id()));
        timeSlotService.upsertTimeSlot(otherUserId, new UpsertTimeSlotRequest(date, 36, "看书", otherCategory.id()));

        List<TimeSlotDto> userSlots = timeSlotService.getDailySlots(userId, date);
        List<TimeSlotDto> otherUserSlots = timeSlotService.getDailySlots(otherUserId, date);

        assertThat(userSlots).hasSize(1);
        assertThat(otherUserSlots).hasSize(1);
        assertThat(userSlots.get(0).activityName()).isEqualTo("写代码");
        assertThat(otherUserSlots.get(0).activityName()).isEqualTo("看书");
    }

    @Test
    @DisplayName("历史记录引用已归档分类时应显示归档标记")
    void should_display_archived_category_for_historical_slots() {
        timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date, 36, "写代码", workCategory.id()));
        categoryService.deleteCategory(userId, workCategory.id());

        List<TimeSlotDto> slots = timeSlotService.getDailySlots(userId, date);

        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).categoryStatus()).isEqualTo("ARCHIVED");
        assertThat(slots.get(0).categoryDisplayName()).isEqualTo("工作 (已归档)");
    }

    @Test
    @DisplayName("不能使用已归档分类保存新的时间格")
    void should_reject_archived_category_when_saving_slot() {
        Category category = categoryRepository.findById(workCategory.id()).orElseThrow();
        category.setStatus(CategoryStatus.ARCHIVED);
        categoryRepository.save(category);

        assertThatThrownBy(() -> timeSlotService.upsertTimeSlot(
            userId,
            new UpsertTimeSlotRequest(date, 37, "继续写代码", workCategory.id())
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Archived category cannot be used for new time slots");
    }

    @Test
    @DisplayName("删除时间格只允许删除当前用户自己的记录")
    void should_delete_only_owner_slot() {
        TimeSlotDto saved = timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date, 36, "写代码", workCategory.id()));

        assertThatThrownBy(() -> timeSlotService.deleteTimeSlot(otherUserId, saved.id()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Time slot not found");

        DeleteTimeSlotResponseDto result = timeSlotService.deleteTimeSlot(userId, saved.id());
        assertThat(result.deleted()).isTrue();
        assertThat(timeSlotService.getDailySlots(userId, date)).isEmpty();
    }
}
