package com.interval.timeslot.dto;

import java.util.List;

public record BatchUpsertTimeSlotResponseDto(
    int savedCount,
    List<TimeSlotDto> slots
) {}
