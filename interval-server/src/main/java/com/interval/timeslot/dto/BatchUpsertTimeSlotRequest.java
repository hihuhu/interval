package com.interval.timeslot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record BatchUpsertTimeSlotRequest(
    @NotNull(message = "date is required")
    LocalDate date,

    @NotEmpty(message = "slots are required")
    @Size(max = 96, message = "slots must be at most 96 items")
    List<@Valid BatchSlotRequest> slots
) {
    public record BatchSlotRequest(
        @NotNull(message = "slotIndex is required")
        @Min(value = 0, message = "slotIndex must be between 0 and 95")
        @Max(value = 95, message = "slotIndex must be between 0 and 95")
        Integer slotIndex,

        @NotBlank(message = "activityName is required")
        @Size(max = 100, message = "activityName must be at most 100 characters")
        String activityName,

        @NotNull(message = "categoryId is required")
        @Positive(message = "categoryId must be positive")
        Long categoryId,

        @Size(max = 500, message = "note must be at most 500 characters")
        String note,

        Boolean noteTouched
    ) {
        public BatchSlotRequest {
            if (activityName != null) {
                activityName = activityName.trim();
            }
            if (note != null) {
                note = note.trim();
            }
            noteTouched = Boolean.TRUE.equals(noteTouched);
        }
    }
}
