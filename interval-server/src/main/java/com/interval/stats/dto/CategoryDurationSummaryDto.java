package com.interval.stats.dto;

import java.time.LocalDate;
import java.util.List;

public record CategoryDurationSummaryDto(
    LocalDate startDate,
    LocalDate endDate,
    Long totalSlotCount,
    Long totalRecordedMinutes,
    Long totalAvailableMinutes,
    Long unrecordedMinutes,
    List<CategoryDurationStatDto> categories
) {
}
