package com.interval.timeslot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpsertTimeSlotRequest(
    @NotNull(message = "date is required")
    LocalDate date,

    @NotNull(message = "slotIndex is required")
    @Min(value = 0, message = "slotIndex must be between 0 and 95")
    @Max(value = 95, message = "slotIndex must be between 0 and 95")
    Integer slotIndex,

    @NotBlank(message = "activityName is required")
    @Size(max = 100, message = "activityName must be at most 100 characters")
    String activityName,

    @NotNull(message = "categoryId is required")
    @Positive(message = "categoryId must be positive")
    Long categoryId
) {
    public UpsertTimeSlotRequest {
        if (activityName != null) {
            activityName = activityName.trim();
        }
    }
}
