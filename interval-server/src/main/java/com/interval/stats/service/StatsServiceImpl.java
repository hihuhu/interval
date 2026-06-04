package com.interval.stats.service;

import com.interval.auth.service.UserActivityService;
import com.interval.stats.dto.CategoryDurationStatDto;
import com.interval.stats.dto.CategoryDurationStatRawDto;
import com.interval.stats.dto.CategoryDurationSummaryDto;
import com.interval.timeslot.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class StatsServiceImpl implements StatsService {

    private static final long MINUTES_PER_SLOT = 15L;
    private static final long SLOTS_PER_DAY = 96L;
    private static final long MAX_RANGE_DAYS = 366L;

    private final TimeSlotRepository timeSlotRepository;
    private final UserActivityService userActivityService;

    public StatsServiceImpl(TimeSlotRepository timeSlotRepository) {
        this(timeSlotRepository, null);
    }

    @Autowired
    public StatsServiceImpl(TimeSlotRepository timeSlotRepository, UserActivityService userActivityService) {
        this.timeSlotRepository = timeSlotRepository;
        this.userActivityService = userActivityService;
    }

    @Override
    public CategoryDurationSummaryDto getCategoryDurations(Long userId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);

        List<CategoryDurationStatRawDto> rawStats = timeSlotRepository.findCategoryDurationStats(userId, startDate, endDate);
        long totalSlotCount = rawStats.stream()
            .mapToLong(CategoryDurationStatRawDto::slotCount)
            .sum();
        long totalRecordedMinutes = totalSlotCount * MINUTES_PER_SLOT;
        long totalAvailableMinutes = inclusiveDayCount(startDate, endDate) * SLOTS_PER_DAY * MINUTES_PER_SLOT;

        List<CategoryDurationStatDto> categories = rawStats.stream()
            .map(raw -> toStatDto(raw, totalRecordedMinutes))
            .toList();

        CategoryDurationSummaryDto summary = new CategoryDurationSummaryDto(
            startDate,
            endDate,
            totalSlotCount,
            totalRecordedMinutes,
            totalAvailableMinutes,
            Math.max(totalAvailableMinutes - totalRecordedMinutes, 0L),
            categories
        );
        if (userActivityService != null) {
            userActivityService.markActive(userId);
        }
        return summary;
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }
        if (inclusiveDayCount(startDate, endDate) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range must not exceed 366 days");
        }
    }

    private CategoryDurationStatDto toStatDto(CategoryDurationStatRawDto raw, long totalRecordedMinutes) {
        long durationMinutes = raw.slotCount() * MINUTES_PER_SLOT;
        return new CategoryDurationStatDto(
            raw.categoryId(),
            raw.categoryName(),
            raw.categoryColor(),
            raw.categoryStatus().name(),
            raw.slotCount(),
            durationMinutes,
            percentage(durationMinutes, totalRecordedMinutes)
        );
    }

    private double percentage(long durationMinutes, long totalRecordedMinutes) {
        if (totalRecordedMinutes == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(durationMinutes)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalRecordedMinutes), 2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private long inclusiveDayCount(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
