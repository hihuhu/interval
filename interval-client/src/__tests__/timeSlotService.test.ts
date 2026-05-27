import { beforeEach, describe, expect, it, vi } from 'vitest';
import { http } from '@/services/http';
import * as timeSlotService from '@/services/timeSlotService';

vi.mock('@/services/http', () => ({
  http: {
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockedHttp = vi.mocked(http);

describe('timeSlotService', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('posts batch upsert payload to the batch endpoint', async () => {
    mockedHttp.post.mockResolvedValue({ savedCount: 1, slots: [] });

    await timeSlotService.upsertTimeSlotBatch({
      date: '2026-05-26',
      slots: [{ slotIndex: 36, activityName: 'Focus', categoryId: 1, note: 'Plan', noteTouched: true }],
    });

    expect(mockedHttp.post).toHaveBeenCalledWith('/api/time-slots/batch', {
      date: '2026-05-26',
      slots: [{ slotIndex: 36, activityName: 'Focus', categoryId: 1, note: 'Plan', noteTouched: true }],
    });
  });

  it('sends batch delete payload through the batch delete endpoint', async () => {
    mockedHttp.delete.mockResolvedValue({ deletedCount: 2, slotIds: [1, 2] });

    await timeSlotService.deleteTimeSlotBatch({ slotIds: [1, 2] });

    expect(mockedHttp.delete).toHaveBeenCalledWith('/api/time-slots/batch', { data: { slotIds: [1, 2] } });
  });
});
