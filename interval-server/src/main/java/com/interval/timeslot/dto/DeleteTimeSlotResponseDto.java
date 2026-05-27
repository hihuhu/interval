package com.interval.timeslot.dto;

public record DeleteTimeSlotResponseDto(
    boolean deleted,
    Long slotId
) {
    public DeleteTimeSlotResponseDto {
        if (slotId == null || slotId <= 0) {
            throw new IllegalArgumentException("slotId must be positive");
        }
    }
}
