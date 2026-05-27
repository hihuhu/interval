package com.interval.timeslot.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchDeleteTimeSlotRequest(
    @NotEmpty(message = "slotIds are required")
    @Size(max = 96, message = "slotIds must be at most 96 items")
    List<Long> slotIds
) {}
