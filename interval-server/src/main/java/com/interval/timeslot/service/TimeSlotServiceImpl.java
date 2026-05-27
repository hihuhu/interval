package com.interval.timeslot.service;

import com.interval.category.entity.Category;
import com.interval.category.entity.CategoryStatus;
import com.interval.category.repository.CategoryRepository;
import com.interval.timeslot.dto.DeleteTimeSlotResponseDto;
import com.interval.timeslot.dto.BatchDeleteTimeSlotRequest;
import com.interval.timeslot.dto.BatchDeleteTimeSlotResponseDto;
import com.interval.timeslot.dto.BatchUpsertTimeSlotRequest;
import com.interval.timeslot.dto.BatchUpsertTimeSlotRequest.BatchSlotRequest;
import com.interval.timeslot.dto.BatchUpsertTimeSlotResponseDto;
import com.interval.timeslot.dto.TimeSlotDto;
import com.interval.timeslot.dto.UpsertTimeSlotRequest;
import com.interval.timeslot.entity.TimeSlot;
import com.interval.timeslot.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TimeSlotServiceImpl implements TimeSlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<TimeSlotDto> getDailySlots(Long userId, LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date cannot be null");
        }
        return timeSlotRepository.findByUserIdAndDateOrderBySlotIndexAsc(userId, date)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    @Transactional
    public TimeSlotDto upsertTimeSlot(Long userId, UpsertTimeSlotRequest request) {
        validateSlotIndex(request.slotIndex());
        Category category = categoryRepository.findByUserIdAndId(userId, request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (category.getStatus() == CategoryStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived category cannot be used for new time slots");
        }

        Instant now = Instant.now();
        TimeSlot slot = timeSlotRepository
            .findByUserIdAndDateAndSlotIndex(userId, request.date(), request.slotIndex())
            .orElseGet(() -> createTimeSlot(userId, request.date(), request.slotIndex(), now));

        slot.setActivityName(request.activityName());
        slot.setNote(request.note());
        slot.setCategory(category);
        slot.setUpdatedAt(now);

        TimeSlot saved = timeSlotRepository.save(slot);
        return toDto(saved);
    }

    @Override
    @Transactional
    public BatchUpsertTimeSlotResponseDto batchUpsertTimeSlots(Long userId, BatchUpsertTimeSlotRequest request) {
        if (request.date() == null) {
            throw new IllegalArgumentException("date cannot be null");
        }

        Instant now = Instant.now();
        List<TimeSlot> slotsToSave = new ArrayList<>();
        for (BatchSlotRequest slotRequest : request.slots()) {
            validateSlotIndex(slotRequest.slotIndex());
            Category category = categoryRepository.findByUserIdAndId(userId, slotRequest.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            if (category.getStatus() == CategoryStatus.ARCHIVED) {
                throw new IllegalArgumentException("Archived category cannot be used for new time slots");
            }

            TimeSlot slot = timeSlotRepository
                .findByUserIdAndDateAndSlotIndex(userId, request.date(), slotRequest.slotIndex())
                .orElseGet(() -> createTimeSlot(userId, request.date(), slotRequest.slotIndex(), now));

            slot.setActivityName(slotRequest.activityName());
            if (slotRequest.noteTouched()) {
                slot.setNote(slotRequest.note() == null || slotRequest.note().isBlank() ? null : slotRequest.note());
            }
            slot.setCategory(category);
            slot.setUpdatedAt(now);
            slotsToSave.add(slot);
        }

        List<TimeSlotDto> saved = timeSlotRepository.saveAll(slotsToSave)
            .stream()
            .map(this::toDto)
            .toList();
        return new BatchUpsertTimeSlotResponseDto(saved.size(), saved);
    }

    @Override
    @Transactional
    public DeleteTimeSlotResponseDto deleteTimeSlot(Long userId, Long slotId) {
        TimeSlot slot = timeSlotRepository.findByIdAndUserId(slotId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Time slot not found"));

        timeSlotRepository.delete(slot);
        return new DeleteTimeSlotResponseDto(true, slotId);
    }

    @Override
    @Transactional
    public BatchDeleteTimeSlotResponseDto batchDeleteTimeSlots(Long userId, BatchDeleteTimeSlotRequest request) {
        List<TimeSlot> slots = timeSlotRepository.findByUserIdAndIdIn(userId, request.slotIds());
        timeSlotRepository.deleteAll(slots);
        List<Long> deletedIds = slots.stream().map(TimeSlot::getId).toList();
        return new BatchDeleteTimeSlotResponseDto(deletedIds.size(), deletedIds);
    }

    private TimeSlot createTimeSlot(Long userId, LocalDate date, Integer slotIndex, Instant now) {
        TimeSlot slot = new TimeSlot();
        slot.setUserId(userId);
        slot.setDate(date);
        slot.setSlotIndex(slotIndex);
        slot.setCreatedAt(now);
        return slot;
    }

    private void validateSlotIndex(Integer slotIndex) {
        if (slotIndex == null || slotIndex < 0 || slotIndex > 95) {
            throw new IllegalArgumentException("slotIndex must be between 0 and 95");
        }
    }

    private TimeSlotDto toDto(TimeSlot slot) {
        Category category = slot.getCategory();
        Long categoryId = null;
        String categoryName = "未知分类";
        String categoryColor = null;
        String categoryStatus = null;
        String categoryDisplayName = "未知分类";

        if (category != null) {
            categoryId = category.getId();
            categoryName = category.getName();
            categoryColor = category.getColorCode();
            categoryStatus = category.getStatus().name();
            categoryDisplayName = category.getStatus() == CategoryStatus.ARCHIVED
                ? category.getName() + " (已归档)"
                : category.getName();
        }

        return new TimeSlotDto(
            slot.getId(),
            slot.getDate(),
            slot.getSlotIndex(),
            slot.getActivityName(),
            slot.getNote(),
            categoryId,
            categoryName,
            categoryColor,
            categoryStatus,
            categoryDisplayName
        );
    }
}
