import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import SelectionEditorPanel from '@/components/SelectionEditorPanel.vue';
import type { CategoryDto } from '@/types/category';
import type { TimeSlotDto } from '@/types/timeSlot';

const categories: CategoryDto[] = [
  { id: 1, name: 'Work', colorCode: '#3b82f6', status: 'ACTIVE', displayOrder: 0 },
  { id: 2, name: 'Study', colorCode: '#22c55e', status: 'ACTIVE', displayOrder: 1 },
];

function slot(slotIndex: number, categoryId: number, note: string | null): TimeSlotDto {
  const category = categories.find((item) => item.id === categoryId)!;
  return {
    id: slotIndex,
    date: '2026-05-26',
    slotIndex,
    activityName: note || category.name,
    note,
    categoryId,
    categoryName: category.name,
    categoryColor: category.colorCode,
    categoryStatus: 'ACTIVE',
    categoryDisplayName: category.name,
  };
}

describe('SelectionEditorPanel', () => {
  it('shows an empty state when no slots are selected', () => {
    const wrapper = mount(SelectionEditorPanel, {
      props: { selectedSlotIndexes: [], slots: [], categories },
    });

    expect(wrapper.text()).toContain('选择时间块');
  });

  it('summarizes selected slots and preloads a shared category and note', async () => {
    const wrapper = mount(SelectionEditorPanel, {
      props: {
        selectedSlotIndexes: [36, 37],
        slots: [slot(36, 1, 'Planning'), slot(37, 1, 'Planning')],
        categories,
      },
    });

    expect(wrapper.text()).toContain('2 个时间块');
    expect(wrapper.text()).toContain('30 分钟');
    expect(wrapper.find('[data-testid="category-option-1"]').classes()).toContain('active');
    expect((wrapper.find('textarea[name="note"]').element as HTMLTextAreaElement).value).toBe('Planning');
  });

  it('keeps mixed notes untouched when saving without editing note', async () => {
    const wrapper = mount(SelectionEditorPanel, {
      props: {
        selectedSlotIndexes: [36, 44],
        slots: [slot(36, 1, 'Planning'), slot(44, 2, 'Reading')],
        categories,
      },
    });

    expect(wrapper.text()).toContain('包含多个分类');
    expect(wrapper.text()).toContain('包含不同备注');
    await wrapper.find('[data-testid="category-option-1"]').trigger('click');
    await wrapper.find('form').trigger('submit.prevent');

    expect(wrapper.emitted('save')?.[0]).toEqual([{
      slotIndexes: [36, 44],
      categoryId: 1,
      note: null,
      noteTouched: false,
    }]);
  });

  it('emits erase for selected recorded slots', async () => {
    const wrapper = mount(SelectionEditorPanel, {
      props: {
        selectedSlotIndexes: [36, 37],
        slots: [slot(36, 1, 'Planning')],
        categories,
      },
    });

    await wrapper.find('[data-testid="erase-selected"]').trigger('click');

    expect(wrapper.emitted('erase')?.[0]).toEqual([[36]]);
  });
});
