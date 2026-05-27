package com.interval.timeslot.dto;

import java.util.List;

public record BatchDeleteTimeSlotResponseDto(
    int deletedCount,
    List<Long> slotIds
) {}
