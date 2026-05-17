package com.interval.timeslot.service;

import com.interval.timeslot.dto.DeleteTimeSlotResponseDto;
import com.interval.timeslot.dto.TimeSlotDto;
import com.interval.timeslot.dto.UpsertTimeSlotRequest;

import java.time.LocalDate;
import java.util.List;

public interface TimeSlotService {

    List<TimeSlotDto> getDailySlots(Long userId, LocalDate date);

    TimeSlotDto upsertTimeSlot(Long userId, UpsertTimeSlotRequest request);

    DeleteTimeSlotResponseDto deleteTimeSlot(Long userId, Long slotId);
}
