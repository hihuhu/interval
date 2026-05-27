import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useTimeSlotStore } from '@/stores/useTimeSlotStore';
import * as timeSlotService from '@/services/timeSlotService';

vi.mock('@/services/timeSlotService');

const mockedTimeSlotService = vi.mocked(timeSlotService);

function slot(id: number, slotIndex: number, activityName = 'Focus') {
  return {
    id,
    date: '2026-05-17',
    slotIndex,
    activityName,
    categoryId: 1,
    note: null,
    categoryName: 'Work',
    categoryColor: '#3b82f6',
    categoryStatus: 'ACTIVE' as const,
    categoryDisplayName: 'Work',
  };
}

describe('useTimeSlotStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.resetAllMocks();
  });

  it('upserts a slot range in one batch and updates local slots', async () => {
    mockedTimeSlotService.upsertTimeSlotBatch.mockResolvedValue({
      savedCount: 3,
      slots: [slot(1, 10), slot(2, 11), slot(3, 12)],
    });

    const store = useTimeSlotStore();
    const result = await store.upsertSlotRange('2026-05-17', [10, 11, 12], 'Focus', 1, {
      note: 'Deep work',
      noteTouched: true,
    });

    expect(mockedTimeSlotService.upsertTimeSlotBatch).toHaveBeenCalledWith({
      date: '2026-05-17',
      slots: [
        { slotIndex: 10, activityName: 'Focus', categoryId: 1, note: 'Deep work', noteTouched: true },
        { slotIndex: 11, activityName: 'Focus', categoryId: 1, note: 'Deep work', noteTouched: true },
        { slotIndex: 12, activityName: 'Focus', categoryId: 1, note: 'Deep work', noteTouched: true },
      ],
    });
    expect(result.map((item) => item.slotIndex)).toEqual([10, 11, 12]);
    expect(store.slots.map((item) => item.slotIndex)).toEqual([10, 11, 12]);
  });

  it('erases selected slots in one batch and removes them locally', async () => {
    mockedTimeSlotService.deleteTimeSlotBatch.mockResolvedValue({ deletedCount: 2, slotIds: [1, 2] });

    const store = useTimeSlotStore();
    store.$patch({ slots: [slot(1, 10), slot(2, 11), slot(3, 12)] });

    const result = await store.deleteSlotBatch([1, 2]);

    expect(mockedTimeSlotService.deleteTimeSlotBatch).toHaveBeenCalledWith({ slotIds: [1, 2] });
    expect(result.deletedCount).toBe(2);
    expect(store.slots.map((item) => item.id)).toEqual([3]);
  });
});
