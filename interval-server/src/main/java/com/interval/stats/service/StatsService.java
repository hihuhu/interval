package com.interval.stats.service;

import com.interval.stats.dto.CategoryDurationSummaryDto;

import java.time.LocalDate;

public interface StatsService {
    CategoryDurationSummaryDto getCategoryDurations(Long userId, LocalDate startDate, LocalDate endDate);
}
