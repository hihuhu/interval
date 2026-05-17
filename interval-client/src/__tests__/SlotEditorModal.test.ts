import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import SlotEditorModal from '@/components/SlotEditorModal.vue';
import type { CategoryDto } from '@/types/category';

const categories: CategoryDto[] = [
  { id: 1, name: 'Work', colorCode: '#3b82f6', status: 'ACTIVE', displayOrder: 0 },
];

describe('SlotEditorModal', () => {
  it('does not render when closed', () => {
    const wrapper = mount(SlotEditorModal, {
      props: { open: false, slotIndex: 1, categories },
    });
    expect(wrapper.text()).toBe('');
  });

  it('emits submit payload with activity and category', async () => {
    window.alert = vi.fn();
    const wrapper = mount(SlotEditorModal, {
      props: { open: true, slotIndex: 36, categories },
    });

    await wrapper.find('input[name="activityName"]').setValue('Write code');
    await wrapper.find('select[name="categoryId"]').setValue('1');
    await wrapper.find('form').trigger('submit.prevent');

    expect(wrapper.emitted('submit')?.[0]).toEqual([
      { slotIndex: 36, slotIndexes: [36], activityName: 'Write code', categoryId: 1 },
    ]);
  });

  it('shows range summary and emits all selected slot indexes', async () => {
    window.alert = vi.fn();
    const wrapper = mount(SlotEditorModal, {
      props: { open: true, slotIndex: 10, slotIndexes: [10, 11, 12], categories },
    });

    expect(wrapper.text()).toContain('3 格');
    await wrapper.find('input[name="activityName"]').setValue('Read');
    await wrapper.find('select[name="categoryId"]').setValue('1');
    await wrapper.find('form').trigger('submit.prevent');

    expect(wrapper.emitted('submit')?.[0]).toEqual([
      { slotIndex: 10, slotIndexes: [10, 11, 12], activityName: 'Read', categoryId: 1 },
    ]);
  });
});
