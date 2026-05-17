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

  it('upserts a slot range sequentially and updates local slots', async () => {
    mockedTimeSlotService.upsertTimeSlot
      .mockResolvedValueOnce(slot(1, 10))
      .mockResolvedValueOnce(slot(2, 11))
      .mockResolvedValueOnce(slot(3, 12));

    const store = useTimeSlotStore();
    const result = await store.upsertSlotRange('2026-05-17', [10, 11, 12], 'Focus', 1);

    expect(mockedTimeSlotService.upsertTimeSlot).toHaveBeenCalledTimes(3);
    expect(mockedTimeSlotService.upsertTimeSlot).toHaveBeenNthCalledWith(1, {
      date: '2026-05-17',
      slotIndex: 10,
      activityName: 'Focus',
      categoryId: 1,
    });
    expect(result.map((item) => item.slotIndex)).toEqual([10, 11, 12]);
    expect(store.slots.map((item) => item.slotIndex)).toEqual([10, 11, 12]);
  });
});
