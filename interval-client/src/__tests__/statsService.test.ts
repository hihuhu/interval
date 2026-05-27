import { beforeEach, describe, expect, it, vi } from 'vitest';
import { http } from '@/services/http';
import * as statsService from '@/services/statsService';

vi.mock('@/services/http', () => ({
  http: {
    get: vi.fn(),
  },
}));

const mockedHttp = vi.mocked(http);

describe('statsService', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('loads category duration stats with date range query params', async () => {
    mockedHttp.get.mockResolvedValue({
      startDate: '2026-05-01',
      endDate: '2026-05-31',
      totalSlotCount: 4,
      totalRecordedMinutes: 60,
      totalAvailableMinutes: 44640,
      unrecordedMinutes: 44580,
      categories: [],
    });

    const result = await statsService.getCategoryDurations('2026-05-01', '2026-05-31');

    expect(mockedHttp.get).toHaveBeenCalledWith('/api/stats/category-durations', {
      params: { startDate: '2026-05-01', endDate: '2026-05-31' },
    });
    expect(result.totalRecordedMinutes).toBe(60);
  });
});
