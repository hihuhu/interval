package com.interval.timeslot;

import com.interval.category.entity.Category;
import com.interval.category.entity.CategoryStatus;
import com.interval.category.repository.CategoryRepository;
import com.interval.timeslot.dto.DeleteTimeSlotResponseDto;
import com.interval.timeslot.dto.BatchDeleteTimeSlotRequest;
import com.interval.timeslot.dto.BatchDeleteTimeSlotResponseDto;
import com.interval.timeslot.dto.BatchUpsertTimeSlotRequest;
import com.interval.timeslot.dto.BatchUpsertTimeSlotResponseDto;
import com.interval.timeslot.dto.BatchUpsertTimeSlotRequest.BatchSlotRequest;
import com.interval.timeslot.dto.TimeSlotDto;
import com.interval.timeslot.dto.UpsertTimeSlotRequest;
import com.interval.timeslot.entity.TimeSlot;
import com.interval.timeslot.repository.TimeSlotRepository;
import com.interval.timeslot.service.TimeSlotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeSlot 服务测试")
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TimeSlotServiceImpl timeSlotService;

    private final Long userId = 1L;
    private final LocalDate date = LocalDate.of(2026, 5, 16);
    private Category workCategory;

    @BeforeEach
    void setUp() {
        workCategory = new Category();
        workCategory.setId(10L);
        workCategory.setUserId(userId);
        workCategory.setName("工作");
        workCategory.setColorCode("#3b82f6");
        workCategory.setStatus(CategoryStatus.ACTIVE);
        workCategory.setDisplayOrder(0);
    }

    @Test
    @DisplayName("查询每日时间格应返回按 slotIndex 排序的 DTO")
    void should_return_daily_slots_ordered_by_slot_index() {
        TimeSlot slot = createSlot(1L, userId, date, 36, "写代码", workCategory);
        when(timeSlotRepository.findByUserIdAndDateOrderBySlotIndexAsc(userId, date)).thenReturn(List.of(slot));

        List<TimeSlotDto> result = timeSlotService.getDailySlots(userId, date);

        assertEquals(1, result.size());
        assertEquals(36, result.get(0).slotIndex());
        assertEquals("写代码", result.get(0).activityName());
        assertEquals("记录设计讨论", result.get(0).note());
        assertEquals("工作", result.get(0).categoryName());
        assertEquals("工作", result.get(0).categoryDisplayName());
        verify(timeSlotRepository).findByUserIdAndDateOrderBySlotIndexAsc(userId, date);
    }

    @Test
    @DisplayName("保存新时间格时应创建记录")
    void should_create_new_slot_when_slot_does_not_exist() {
        UpsertTimeSlotRequest request = new UpsertTimeSlotRequest(date, 36, "写代码", workCategory.getId());
        TimeSlot savedSlot = createSlot(1L, userId, date, 36, "写代码", workCategory);

        when(categoryRepository.findByUserIdAndId(userId, workCategory.getId())).thenReturn(Optional.of(workCategory));
        when(timeSlotRepository.findByUserIdAndDateAndSlotIndex(userId, date, 36)).thenReturn(Optional.empty());
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(savedSlot);

        TimeSlotDto result = timeSlotService.upsertTimeSlot(userId, request);

        assertEquals(1L, result.id());
        assertEquals(36, result.slotIndex());
        assertEquals("写代码", result.activityName());
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    @DisplayName("批量保存时间格时应保留未触碰备注并覆盖主动编辑的备注")
    void should_batch_upsert_slots_and_respect_note_touched_flag() {
        TimeSlot existingSlot = createSlot(1L, userId, date, 36, "写代码", workCategory);
        existingSlot.setNote("原备注");
        TimeSlot savedExisting = createSlot(1L, userId, date, 36, "计划", workCategory);
        savedExisting.setNote("原备注");
        TimeSlot savedNew = createSlot(2L, userId, date, 37, "计划", workCategory);
        savedNew.setNote("新备注");
        BatchUpsertTimeSlotRequest request = new BatchUpsertTimeSlotRequest(
            date,
            List.of(
                new BatchSlotRequest(36, "计划", workCategory.getId(), null, false),
                new BatchSlotRequest(37, "计划", workCategory.getId(), "新备注", true)
            )
        );

        when(categoryRepository.findByUserIdAndId(userId, workCategory.getId())).thenReturn(Optional.of(workCategory));
        when(timeSlotRepository.findByUserIdAndDateAndSlotIndex(userId, date, 36)).thenReturn(Optional.of(existingSlot));
        when(timeSlotRepository.findByUserIdAndDateAndSlotIndex(userId, date, 37)).thenReturn(Optional.empty());
        when(timeSlotRepository.saveAll(any())).thenReturn(List.of(savedExisting, savedNew));

        BatchUpsertTimeSlotResponseDto result = timeSlotService.batchUpsertTimeSlots(userId, request);

        assertEquals(2, result.savedCount());
        assertEquals("原备注", result.slots().get(0).note());
        assertEquals("新备注", result.slots().get(1).note());
        verify(timeSlotRepository).saveAll(any());
    }

    @Test
    @DisplayName("批量删除只删除当前用户拥有的时间格")
    void should_batch_delete_only_owned_slots() {
        TimeSlot first = createSlot(1L, userId, date, 36, "写代码", workCategory);
        TimeSlot second = createSlot(2L, userId, date, 37, "写代码", workCategory);
        BatchDeleteTimeSlotRequest request = new BatchDeleteTimeSlotRequest(List.of(1L, 2L, 99L));
        when(timeSlotRepository.findByUserIdAndIdIn(userId, request.slotIds())).thenReturn(List.of(first, second));

        BatchDeleteTimeSlotResponseDto result = timeSlotService.batchDeleteTimeSlots(userId, request);

        assertEquals(2, result.deletedCount());
        assertEquals(List.of(1L, 2L), result.slotIds());
        verify(timeSlotRepository).deleteAll(List.of(first, second));
    }

    @Test
    @DisplayName("保存已有时间格时应覆盖原记录")
    void should_update_existing_slot_when_slot_exists() {
        UpsertTimeSlotRequest request = new UpsertTimeSlotRequest(date, 36, "改 Bug", workCategory.getId());
        TimeSlot existingSlot = createSlot(1L, userId, date, 36, "写代码", workCategory);

        when(categoryRepository.findByUserIdAndId(userId, workCategory.getId())).thenReturn(Optional.of(workCategory));
        when(timeSlotRepository.findByUserIdAndDateAndSlotIndex(userId, date, 36)).thenReturn(Optional.of(existingSlot));
        when(timeSlotRepository.save(existingSlot)).thenReturn(existingSlot);

        TimeSlotDto result = timeSlotService.upsertTimeSlot(userId, request);

        assertEquals(1L, result.id());
        assertEquals("改 Bug", result.activityName());
        assertSame(workCategory, existingSlot.getCategory());
        verify(timeSlotRepository).save(existingSlot);
    }

    @Test
    @DisplayName("slotIndex 小于 0 时应拒绝保存")
    void should_reject_slot_index_less_than_zero() {
        UpsertTimeSlotRequest request = new UpsertTimeSlotRequest(date, -1, "写代码", workCategory.getId());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> timeSlotService.upsertTimeSlot(userId, request)
        );

        assertEquals("slotIndex must be between 0 and 95", exception.getMessage());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("slotIndex 大于 95 时应拒绝保存")
    void should_reject_slot_index_greater_than_95() {
        UpsertTimeSlotRequest request = new UpsertTimeSlotRequest(date, 96, "写代码", workCategory.getId());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> timeSlotService.upsertTimeSlot(userId, request)
        );

        assertEquals("slotIndex must be between 0 and 95", exception.getMessage());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("分类不属于当前用户时应拒绝保存")
    void should_reject_category_not_owned_by_user() {
        UpsertTimeSlotRequest request = new UpsertTimeSlotRequest(date, 36, "写代码", workCategory.getId());
        when(categoryRepository.findByUserIdAndId(userId, workCategory.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> timeSlotService.upsertTimeSlot(userId, request)
        );

        assertEquals("Category not found", exception.getMessage());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("已归档分类不能用于新增或覆盖时间格")
    void should_reject_archived_category() {
        workCategory.setStatus(CategoryStatus.ARCHIVED);
        UpsertTimeSlotRequest request = new UpsertTimeSlotRequest(date, 36, "写代码", workCategory.getId());
        when(categoryRepository.findByUserIdAndId(userId, workCategory.getId())).thenReturn(Optional.of(workCategory));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> timeSlotService.upsertTimeSlot(userId, request)
        );

        assertEquals("Archived category cannot be used for new time slots", exception.getMessage());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("删除当前用户时间格应成功")
    void should_delete_slot_owned_by_user() {
        TimeSlot slot = createSlot(1L, userId, date, 36, "写代码", workCategory);
        when(timeSlotRepository.findByIdAndUserId(slot.getId(), userId)).thenReturn(Optional.of(slot));

        DeleteTimeSlotResponseDto result = timeSlotService.deleteTimeSlot(userId, slot.getId());

        assertTrue(result.deleted());
        assertEquals(slot.getId(), result.slotId());
        verify(timeSlotRepository).delete(slot);
    }

    @Test
    @DisplayName("删除不存在或不属于当前用户的时间格应抛出异常")
    void should_throw_when_deleting_missing_slot() {
        when(timeSlotRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> timeSlotService.deleteTimeSlot(userId, 99L)
        );

        assertEquals("Time slot not found", exception.getMessage());
        verify(timeSlotRepository, never()).delete(any());
    }

    private TimeSlot createSlot(Long id, Long ownerId, LocalDate slotDate, Integer slotIndex, String activityName, Category category) {
        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        slot.setUserId(ownerId);
        slot.setDate(slotDate);
        slot.setSlotIndex(slotIndex);
        slot.setActivityName(activityName);
        slot.setNote("记录设计讨论");
        slot.setCategory(category);
        return slot;
    }
}
