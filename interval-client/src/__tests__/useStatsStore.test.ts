import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useStatsStore } from '@/stores/useStatsStore';
import * as statsService from '@/services/statsService';

vi.mock('@/services/statsService');

const mockedStatsService = vi.mocked(statsService);

const summary = {
  startDate: '2026-05-18',
  endDate: '2026-05-24',
  totalSlotCount: 6,
  totalRecordedMinutes: 90,
  totalAvailableMinutes: 10080,
  unrecordedMinutes: 9990,
  categories: [
    {
      categoryId: 1,
      categoryName: '工作',
      categoryColor: '#6366f1',
      categoryStatus: 'ACTIVE' as const,
      slotCount: 4,
      durationMinutes: 60,
      percentage: 66.67,
    },
  ],
};

describe('useStatsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.resetAllMocks();
  });

  it('loads category duration summary for the selected range', async () => {
    mockedStatsService.getCategoryDurations.mockResolvedValue(summary);

    const store = useStatsStore();
    await store.fetchCategoryDurations('2026-05-18', '2026-05-24', 'week');

    expect(mockedStatsService.getCategoryDurations).toHaveBeenCalledWith('2026-05-18', '2026-05-24');
    expect(store.summary?.totalRecordedMinutes).toBe(90);
    expect(store.selectedRange).toBe('week');
    expect(store.errorMessage).toBeNull();
  });

  it('stores error message and keeps the previous summary when loading fails', async () => {
    mockedStatsService.getCategoryDurations.mockResolvedValueOnce(summary);
    mockedStatsService.getCategoryDurations.mockRejectedValueOnce(new Error('Network failed'));

    const store = useStatsStore();
    await store.fetchCategoryDurations('2026-05-18', '2026-05-24', 'week');

    await expect(store.fetchCategoryDurations('2026-05-01', '2026-05-31', 'month')).rejects.toThrow('Network failed');

    expect(store.summary).toEqual(summary);
    expect(store.errorMessage).toBe('Network failed');
    expect(store.loading).toBe(false);
  });
});
