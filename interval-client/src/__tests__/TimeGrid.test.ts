import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import TimeGrid from '@/components/TimeGrid.vue';
import type { TimeSlotDto } from '@/types/timeSlot';

const slot: TimeSlotDto = {
  id: 1,
  date: '2026-05-16',
  slotIndex: 36,
  activityName: 'Write code',
  categoryId: 1,
  categoryName: 'Work',
  categoryColor: '#3b82f6',
  categoryStatus: 'ACTIVE',
  categoryDisplayName: 'Work',
};

describe('TimeGrid', () => {
  it('renders 96 cells', () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    expect(wrapper.findAll('[data-testid="time-slot-cell"]')).toHaveLength(96);
  });

  it('displays occupied slot content', () => {
    const wrapper = mount(TimeGrid, { props: { slots: [slot] } });
    expect(wrapper.text()).toContain('Write code');
    expect(wrapper.text()).toContain('Work');
  });

  it('emits selectSlot when a cell is clicked', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    await wrapper.findAll('[data-testid="time-slot-cell"]')[12].trigger('click');
    expect(wrapper.emitted('selectSlot')?.[0]).toEqual([12]);
  });

  it('emits selectSlotRange when a cell is shift-clicked after an anchor click', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');

    await cells[10].trigger('click');
    await cells[13].trigger('click', { shiftKey: true });

    expect(wrapper.emitted('selectSlotRange')?.[0]).toEqual([[10, 11, 12, 13]]);
  });
});
