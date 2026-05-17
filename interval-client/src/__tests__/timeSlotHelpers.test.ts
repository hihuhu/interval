import { describe, expect, it } from 'vitest';
import { slotIndexRange, slotIndexToRange, todayIsoDate } from '@/composables/useTimeSlots';

describe('time slot helpers', () => {
  it('maps first slot to midnight range', () => {
    expect(slotIndexToRange(0)).toBe('00:00-00:15');
  });

  it('maps last slot to end of day range', () => {
    expect(slotIndexToRange(95)).toBe('23:45-24:00');
  });

  it('returns today as an ISO date', () => {
    expect(todayIsoDate()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('creates an inclusive sorted slot index range', () => {
    expect(slotIndexRange(5, 8)).toEqual([5, 6, 7, 8]);
    expect(slotIndexRange(8, 5)).toEqual([5, 6, 7, 8]);
  });
});
