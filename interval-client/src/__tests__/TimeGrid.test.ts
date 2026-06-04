import { afterEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';
import { mount } from '@vue/test-utils';
import TimeGrid from '@/components/TimeGrid.vue';
import timeGridSource from '@/components/TimeGrid.vue?raw';
import timeSlotCellSource from '@/components/TimeSlotCell.vue?raw';
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

  it('deduplicates repeated category text in occupied cell tooltip and aria label', () => {
    const duplicateSlot = {
      ...slot,
      activityName: '运动',
      categoryName: '运动',
      categoryDisplayName: '运动',
    };
    const wrapper = mount(TimeGrid, { props: { slots: [duplicateSlot] } });
    const occupiedCell = wrapper.find('[data-slot-index="36"]');

    expect(occupiedCell.attributes('title')).toBe('09:00-09:15 / 运动');
    expect(occupiedCell.attributes('aria-label')).toBe('09:00-09:15 运动');
    expect(occupiedCell.attributes('title')).not.toContain('运动 / 运动');
  });

  it('uses a soft time-board treatment instead of white table cells', () => {
    expect(timeGridSource).toContain('radial-gradient');
    expect(timeGridSource).toContain('--time-board-surface');
    expect(timeSlotCellSource).not.toContain('background: #ffffff;');
    expect(timeSlotCellSource).toContain('--empty-slot-surface');
    expect(timeSlotCellSource).toContain('outline-offset: -2px;');
  });

  it('keeps the grid body in a focused scroll region with quick scroll controls', () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });

    expect(wrapper.find('[data-testid="jump-to-now"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="jump-to-top"]').exists()).toBe(true);
    expect(wrapper.find('.time-board-scroll').attributes('tabindex')).toBe('0');
    expect(timeGridSource).toContain('overscroll-behavior: contain;');
    expect(timeGridSource).toContain('scrollbar-gutter: stable;');
  });

  it('keeps selected cells visually selected on hover with lighter motion costs', () => {
    expect(timeSlotCellSource).toContain('.slot-cell.selected:hover');
    expect(timeSlotCellSource).not.toContain('transform: translateY(-1px);');
    expect(timeSlotCellSource).not.toContain('transition: box-shadow .16s ease, border-color .16s ease, background .16s ease, transform .16s ease;');
  });

  it('keeps slot cell painting light enough for smooth scrolling', () => {
    expect(timeSlotCellSource).not.toContain('.slot-cell::before');
    expect(timeSlotCellSource).not.toContain('box-shadow:');
    expect(timeSlotCellSource).toContain('contain: paint;');
    expect(timeSlotCellSource).toContain('content-visibility: auto;');
  });

  it('emits selectionChange when clicked slots are toggled', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');

    await cells[12].trigger('click');
    await cells[20].trigger('click');
    await cells[12].trigger('click');

    expect(wrapper.emitted('selectionChange')?.map((event) => event[0])).toEqual([[12], [12, 20], [20]]);
  });

  it('clears internal selected cells when the parent clears the selection', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [], selectedSlotIndexes: [12] } });

    expect(wrapper.findAll('[data-testid="time-slot-cell"]')[12].classes()).toContain('selected');

    await wrapper.setProps({ selectedSlotIndexes: [] });

    expect(wrapper.findAll('[data-testid="time-slot-cell"]')[12].classes()).not.toContain('selected');
  });

  it('does not restore a cleared parent selection on the next click', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [], selectedSlotIndexes: [] } });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');

    await cells[12].trigger('click');
    await wrapper.setProps({ selectedSlotIndexes: [12] });
    await wrapper.setProps({ selectedSlotIndexes: [] });
    await cells[20].trigger('click');

    const events = wrapper.emitted('selectionChange') ?? [];
    expect(events[events.length - 1]).toEqual([[20]]);
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

  it('ignores compatibility mouse drag events when pointer events are available', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] } });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');

    await cells[30].trigger('pointerdown');
    await cells[33].trigger('pointerenter');
    await cells[33].trigger('pointerup');
    cells[30].element.dispatchEvent(mouseEvent('mousedown', { button: 0, buttons: 1 }));
    cells[33].element.dispatchEvent(mouseEvent('mouseenter', { buttons: 1 }));
    cells[33].element.dispatchEvent(mouseEvent('mouseup', { button: 0 }));

    expect(wrapper.emitted('selectionChange')).toEqual([[[30, 31, 32, 33]]]);
  });

  it('updates drag selection from grid pointer coordinates', async () => {
    const wrapper = mount(TimeGrid, { props: { slots: [] }, attachTo: document.body });
    const cells = wrapper.findAll('[data-testid="time-slot-cell"]');
    Object.defineProperty(document, 'elementFromPoint', {
      configurable: true,
      value: vi.fn().mockReturnValue(cells[33].element),
    });

    await cells[30].trigger('pointerdown');

    const moveEvent = new MouseEvent('pointermove', { bubbles: true, clientX: 120, clientY: 120 });
    Object.defineProperty(moveEvent, 'buttons', { configurable: true, value: 1 });
    wrapper.find('.time-board').element.dispatchEvent(moveEvent);
    await nextTick();

    await cells[33].trigger('pointerup');

    const events = wrapper.emitted('selectionChange') ?? [];
    expect(events[events.length - 1]).toEqual([[30, 31, 32, 33]]);
    wrapper.unmount();
  });
});

function mouseEvent(type: string, options: MouseEventInit & { buttons?: number }) {
  const event = new MouseEvent(type, { bubbles: true, ...options });
  if (options.buttons !== undefined) {
    Object.defineProperty(event, 'buttons', { configurable: true, value: options.buttons });
  }
  return event;
}
