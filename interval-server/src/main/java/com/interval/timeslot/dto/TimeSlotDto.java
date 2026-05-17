package com.interval.timeslot.dto;

import java.time.LocalDate;

public record TimeSlotDto(
    Long id,
    LocalDate date,
    Integer slotIndex,
    String activityName,
    Long categoryId,
    String categoryName,
    String categoryColor,
    String categoryStatus,
    String categoryDisplayName
) {
    public TimeSlotDto {
        if (date == null) {
            throw new IllegalArgumentException("date cannot be null");
        }
        if (slotIndex == null || slotIndex < 0 || slotIndex > 95) {
            throw new IllegalArgumentException("slotIndex must be between 0 and 95");
        }
        if (activityName == null || activityName.isBlank()) {
            throw new IllegalArgumentException("activityName cannot be blank");
        }
    }
}
