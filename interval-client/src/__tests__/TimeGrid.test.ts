import { afterEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';
import { mount } from '@vue/test-utils';
import TimeGrid from '@/components/TimeGrid.vue';
import type { TimeSlotDto } from '@/types/timeSlot';

const slot: TimeSlotDto = {
  id: 1,
  date: '2026-05-16',
  slotIndex: 36,
  activityName: 'Write code',
  categoryId: 1,
  note: null,
  categoryName: 'Work',
  categoryColor: '#3b82f6',
  categoryStatus: 'ACTIVE',
  categoryDisplayName: 'Work',
};

describe('TimeGrid', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders 96 cells', () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    expect(wrapper.findAll('[data-testid="time-slot-cell"]')).toHaveLength(96);
  });

  it('shows compact prototype-style slot content', () => {
    const wrapper = mount(TimeGrid, { props: { slots: [slot] } });
    const occupiedCell = wrapper.find('[data-slot-index="36"]');
    const blankCell = wrapper.find('[data-slot-index="0"]');

    expect(occupiedCell.text()).toBe('Work');
    expect(occupiedCell.attributes('aria-label')).toContain('09:00-09:15');
    expect(occupiedCell.attributes('aria-label')).toContain('Write code');
    expect(blankCell.text()).toBe('');
  });

  it('emits selectionChange when clicked slots are toggled', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');

    await cells[12].trigger('click');
    await cells[20].trigger('click');
    await cells[12].trigger('click');

    expect(wrapper.emitted('selectionChange')?.map((event) => event[0])).toEqual([[12], [12, 20], [20]]);
  });

  it('clears clicked selection when drag selecting a new continuous range', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');

    await cells[8].trigger('click');
    await cells[30].trigger('mousedown');
    await cells[33].trigger('mouseenter');
    await cells[33].trigger('mouseup');

    const events = wrapper.emitted('selectionChange') ?? [];
    expect(events[events.length - 1]).toEqual([[30, 31, 32, 33]]);
  });

  it('keeps the dragged range when the browser fires click after pointer release', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');

    await cells[30].trigger('pointerdown');
    await cells[33].trigger('pointerenter');
    await cells[33].trigger('pointerup');
    await cells[33].trigger('click');

    const events = wrapper.emitted('selectionChange') ?? [];
    expect(events[events.length - 1]).toEqual([[30, 31, 32, 33]]);
  });

  it('allows clicked additions after pointer drag selection', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');

    await cells[8].trigger('pointerdown');
    await cells[10].trigger('pointerenter');
    await cells[10].trigger('pointerup');
    await cells[32].trigger('click');

    const events = wrapper.emitted('selectionChange') ?? [];
    expect(events[events.length - 1]).toEqual([[8, 9, 10, 32]]);
  });

  it('keeps a dragged range when a real browser click starts with pointerdown', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');

    await cells[8].trigger('pointerdown');
    await cells[10].trigger('pointerenter');
    await cells[10].trigger('pointerup');
    await cells[32].trigger('pointerdown');
    await cells[32].trigger('pointerup');
    await cells[32].trigger('click');

    const events = wrapper.emitted('selectionChange') ?? [];
    expect(events[events.length - 1]).toEqual([[8, 9, 10, 32]]);
  });

  it('updates drag selection from grid pointer coordinates', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] }, attachTo: document.body });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');
    Object.defineProperty(document, 'elementFromPoint', {
      configurable: true,
      value: vi.fn().mockReturnValue(cells[33].element),
    });

    await cells[30].trigger('pointerdown');

    const moveEvent = new MouseEvent('mousemove', { bubbles: true, clientX: 120, clientY: 120 });
    Object.defineProperty(moveEvent, 'buttons', { configurable: true, value: 1 });
    wrapper.find('.time-board').element.dispatchEvent(moveEvent);
    await nextTick();

    await cells[33].trigger('pointerup');

    const events = wrapper.emitted('selectionChange') ?? [];
    expect(events[events.length - 1]).toEqual([[30, 31, 32, 33]]);
    wrapper.unmount();
  });
});
