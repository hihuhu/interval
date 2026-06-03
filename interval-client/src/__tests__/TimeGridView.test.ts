import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import TimeGridView from '@/views/TimeGridView.vue';
import { useAuthStore } from '@/stores/useAuthStore';
import { useCategoryStore } from '@/stores/useCategoryStore';
import { useTimeSlotStore } from '@/stores/useTimeSlotStore';

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { template: '<a><slot /></a>' },
}));

describe('TimeGridView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.$patch({ username: 'admin', token: 'token' });

    const categoryStore = useCategoryStore();
    categoryStore.$patch({
      categories: [
        { id: 1, name: '工作', colorCode: '#3b82f6', status: 'ACTIVE', displayOrder: 0 },
      ],
    });
    vi.spyOn(categoryStore, 'fetchCategories').mockResolvedValue(undefined);

    const timeSlotStore = useTimeSlotStore();
    timeSlotStore.$patch({ selectedDate: '2026-05-28', slots: [] });
    vi.spyOn(timeSlotStore, 'fetchDailySlots').mockResolvedValue(undefined);
  });

  it('uses compact icon date controls that update the selected date', async () => {
    const wrapper = mount(TimeGridView);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(wrapper.text()).not.toContain('前一天');
    expect(wrapper.text()).not.toContain('后一天');

    await wrapper.find('[data-testid="previous-day"]').trigger('click');
    const timeSlotStore = useTimeSlotStore();

    expect(timeSlotStore.setDate).toBeDefined();
    expect(timeSlotStore.selectedDate).toBe('2026-05-27');
    expect(timeSlotStore.fetchDailySlots).toHaveBeenLastCalledWith('2026-05-27');
  });

  it('opens a styled calendar popover from the date trigger', async () => {
    const wrapper = mount(TimeGridView);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(wrapper.find('[data-testid="date-popover"]').exists()).toBe(false);

    await wrapper.find('[data-testid="date-trigger"]').trigger('click');

    expect(wrapper.find('[data-testid="date-popover"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="native-date-input"]').exists()).toBe(false);
  });

  it('clears selected time cells when the editor cancel button is clicked', async () => {
    const wrapper = mount(TimeGridView);
    await new Promise((resolve) => setTimeout(resolve, 0));

    await wrapper.find('[data-slot-index="8"]').trigger('click');

    expect(wrapper.text()).toContain('1 个时间块');
    expect(wrapper.find('[data-slot-index="8"]').classes()).toContain('selected');

    await wrapper.find('[aria-label="取消选择"]').trigger('click');

    expect(wrapper.text()).toContain('先选一个时间块');
    expect(wrapper.find('[data-slot-index="8"]').classes()).not.toContain('selected');
  });

  it('opens category management in a dialog from the editor panel', async () => {
    const wrapper = mount(TimeGridView);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(wrapper.find('[role="dialog"][aria-labelledby="category-manager-title"]').exists()).toBe(false);

    await wrapper.find('[data-testid="open-category-manager"]').trigger('click');

    expect(wrapper.find('[role="dialog"][aria-labelledby="category-manager-title"]').exists()).toBe(true);

    await wrapper.find('[data-testid="close-category-manager"]').trigger('click');

    expect(wrapper.find('[role="dialog"][aria-labelledby="category-manager-title"]').exists()).toBe(false);
  });
});
