package com.interval.stats;

import com.interval.category.dto.CategoryDto;
import com.interval.category.service.CategoryService;
import com.interval.stats.dto.CategoryDurationSummaryDto;
import com.interval.stats.service.StatsService;
import com.interval.timeslot.dto.UpsertTimeSlotRequest;
import com.interval.timeslot.repository.TimeSlotRepository;
import com.interval.timeslot.service.TimeSlotService;
import com.interval.category.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Stats 集成测试")
class StatsIntegrationTest {

    @Autowired
    private StatsService statsService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private final Long userId = 1L;
    private final Long otherUserId = 2L;
    private final LocalDate date = LocalDate.of(2026, 5, 18);
    private CategoryDto workCategory;
    private CategoryDto studyCategory;

    @BeforeEach
    void setUp() {
        timeSlotRepository.deleteAll();
        categoryRepository.deleteAll();
        workCategory = categoryService.createCategory(userId, "工作", "#6366f1");
        studyCategory = categoryService.createCategory(userId, "学习", "#22c55e");
    }

    @Test
    @DisplayName("应只统计当前用户在日期范围内的分类耗时")
    void should_aggregate_only_current_user_slots_in_range() {
        CategoryDto otherCategory = categoryService.createCategory(otherUserId, "他人工作", "#f97316");
        timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date, 36, "写代码", workCategory.id()));
        timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date, 37, "写代码", workCategory.id()));
        timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date, 38, "阅读", studyCategory.id()));
        timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date.plusDays(3), 39, "范围外", studyCategory.id()));
        timeSlotService.upsertTimeSlot(otherUserId, new UpsertTimeSlotRequest(date, 36, "他人记录", otherCategory.id()));

        CategoryDurationSummaryDto result = statsService.getCategoryDurations(userId, date, date);

        assertThat(result.totalSlotCount()).isEqualTo(3L);
        assertThat(result.totalRecordedMinutes()).isEqualTo(45L);
        assertThat(result.categories()).hasSize(2);
        assertThat(result.categories()).extracting("categoryName").containsExactly("工作", "学习");
        assertThat(result.categories().get(0).durationMinutes()).isEqualTo(30L);
        assertThat(result.categories().get(0).percentage()).isEqualTo(66.67);
        assertThat(result.categories().get(1).durationMinutes()).isEqualTo(15L);
        assertThat(result.categories().get(1).percentage()).isEqualTo(33.33);
    }

    @Test
    @DisplayName("已归档分类应保留在统计结果中")
    void should_include_archived_category_in_stats() {
        timeSlotService.upsertTimeSlot(userId, new UpsertTimeSlotRequest(date, 36, "复盘", workCategory.id()));
        categoryService.deleteCategory(userId, workCategory.id());

        CategoryDurationSummaryDto result = statsService.getCategoryDurations(userId, date, date);

        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().get(0).categoryName()).isEqualTo("工作");
        assertThat(result.categories().get(0).categoryStatus()).isEqualTo("ARCHIVED");
    }
}
