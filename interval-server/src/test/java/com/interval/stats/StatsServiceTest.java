package com.interval.stats;

import com.interval.category.entity.CategoryStatus;
import com.interval.stats.dto.CategoryDurationSummaryDto;
import com.interval.stats.dto.CategoryDurationStatRawDto;
import com.interval.stats.service.StatsServiceImpl;
import com.interval.timeslot.repository.TimeSlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stats 服务测试")
class StatsServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    private final Long userId = 1L;
    private final LocalDate startDate = LocalDate.of(2026, 5, 18);
    private final LocalDate endDate = LocalDate.of(2026, 5, 19);

    @Test
    @DisplayName("应按分类聚合时间格数量、分钟数和占比")
    void should_aggregate_category_duration_stats() {
        when(timeSlotRepository.findCategoryDurationStats(userId, startDate, endDate))
            .thenReturn(List.of(
                new CategoryDurationStatRawDto(10L, "工作", "#6366f1", CategoryStatus.ACTIVE, 0, 4L),
                new CategoryDurationStatRawDto(20L, "学习", "#22c55e", CategoryStatus.ACTIVE, 1, 2L)
            ));

        CategoryDurationSummaryDto result = statsService.getCategoryDurations(userId, startDate, endDate);

        assertThat(result.totalSlotCount()).isEqualTo(6L);
        assertThat(result.totalRecordedMinutes()).isEqualTo(90L);
        assertThat(result.totalAvailableMinutes()).isEqualTo(2880L);
        assertThat(result.unrecordedMinutes()).isEqualTo(2790L);
        assertThat(result.categories()).hasSize(2);
        assertThat(result.categories().get(0).categoryName()).isEqualTo("工作");
        assertThat(result.categories().get(0).durationMinutes()).isEqualTo(60L);
        assertThat(result.categories().get(0).percentage()).isEqualTo(66.67);
        assertThat(result.categories().get(1).durationMinutes()).isEqualTo(30L);
        assertThat(result.categories().get(1).percentage()).isEqualTo(33.33);
    }

    @Test
    @DisplayName("已归档分类仍应参与历史统计")
    void should_include_archived_categories() {
        when(timeSlotRepository.findCategoryDurationStats(userId, startDate, startDate))
            .thenReturn(List.of(
                new CategoryDurationStatRawDto(30L, "项目复盘", "#8b5cf6", CategoryStatus.ARCHIVED, 2, 3L)
            ));

        CategoryDurationSummaryDto result = statsService.getCategoryDurations(userId, startDate, startDate);

        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().get(0).categoryStatus()).isEqualTo("ARCHIVED");
        assertThat(result.categories().get(0).durationMinutes()).isEqualTo(45L);
    }

    @Test
    @DisplayName("空范围应返回 0 记录和正确未记录分钟数")
    void should_return_empty_summary_when_no_slots_exist() {
        when(timeSlotRepository.findCategoryDurationStats(userId, startDate, startDate)).thenReturn(List.of());

        CategoryDurationSummaryDto result = statsService.getCategoryDurations(userId, startDate, startDate);

        assertThat(result.totalSlotCount()).isZero();
        assertThat(result.totalRecordedMinutes()).isZero();
        assertThat(result.totalAvailableMinutes()).isEqualTo(1440L);
        assertThat(result.unrecordedMinutes()).isEqualTo(1440L);
        assertThat(result.categories()).isEmpty();
    }

    @Test
    @DisplayName("开始日期晚于结束日期时应拒绝查询")
    void should_reject_start_date_after_end_date() {
        assertThatThrownBy(() -> statsService.getCategoryDurations(userId, endDate, startDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("startDate must be before or equal to endDate");

        verify(timeSlotRepository, never()).findCategoryDurationStats(userId, endDate, startDate);
    }

    @Test
    @DisplayName("日期范围超过 366 天时应拒绝查询")
    void should_reject_range_larger_than_366_days() {
        LocalDate tooLargeEndDate = startDate.plusDays(366);

        assertThatThrownBy(() -> statsService.getCategoryDurations(userId, startDate, tooLargeEndDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Date range must not exceed 366 days");

        verify(timeSlotRepository, never()).findCategoryDurationStats(userId, startDate, tooLargeEndDate);
    }
}
