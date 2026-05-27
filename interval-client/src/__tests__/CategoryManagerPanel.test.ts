import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import CategoryManagerPanel from '@/components/CategoryManagerPanel.vue';
import type { CategoryDto } from '@/types/category';

const categories: CategoryDto[] = [
  { id: 1, name: 'Work', colorCode: '#3b82f6', status: 'ACTIVE', displayOrder: 0 },
];

describe('CategoryManagerPanel', () => {
  it('emits create with trimmed name and selected color', async () => {
    const wrapper = mount(CategoryManagerPanel, { props: { categories } });

    await wrapper.find('input[name="newCategoryName"]').setValue('  Reading  ');
    await wrapper.find('input[name="newCategoryColor"]').setValue('#22c55e');
    await wrapper.find('[data-testid="create-category"]').trigger('submit.prevent');

    expect(wrapper.emitted('create')?.[0]).toEqual([{ name: 'Reading', colorCode: '#22c55e' }]);
  });

  it('emits update with edited draft values', async () => {
    const wrapper = mount(CategoryManagerPanel, { props: { categories } });

    await wrapper.find('input[name="categoryName-1"]').setValue('Deep Work');
    await wrapper.find('[data-testid="update-category-1"]').trigger('submit.prevent');

    expect(wrapper.emitted('update')?.[0]).toEqual([1, {
      name: 'Deep Work',
      colorCode: '#3b82f6',
      displayOrder: 0,
    }]);
  });

  it('emits delete with category id and name', async () => {
    const wrapper = mount(CategoryManagerPanel, { props: { categories } });

    await wrapper.find('[data-testid="delete-category-1"]').trigger('click');

    expect(wrapper.emitted('delete')?.[0]).toEqual([1, 'Work']);
  });
});
