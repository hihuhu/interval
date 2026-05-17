import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useCategoryStore } from '@/stores/useCategoryStore';
import * as categoryService from '@/services/categoryService';

vi.mock('@/services/categoryService');

const mockedCategoryService = vi.mocked(categoryService);

const initialCategories = [
  { id: 1, name: 'Work', colorCode: '#3b82f6', status: 'ACTIVE' as const, displayOrder: 2 },
  { id: 2, name: 'Study', colorCode: '#22c55e', status: 'ACTIVE' as const, displayOrder: 1 },
];

describe('useCategoryStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.resetAllMocks();
  });

  it('updates a category and keeps categories sorted by display order', async () => {
    mockedCategoryService.updateCategory.mockResolvedValue({
      id: 1,
      name: 'Deep Work',
      colorCode: '#6366f1',
      status: 'ACTIVE',
      displayOrder: 2,
    });

    const store = useCategoryStore();
    store.$patch({ categories: initialCategories });

    const updated = await store.updateCategory(1, {
      name: 'Deep Work',
      colorCode: '#6366f1',
      displayOrder: 2,
    });

    expect(mockedCategoryService.updateCategory).toHaveBeenCalledWith(1, {
      name: 'Deep Work',
      colorCode: '#6366f1',
      displayOrder: 2,
    });
    expect(updated.name).toBe('Deep Work');
    expect(store.categories.map((category) => category.id)).toEqual([2, 1]);
    expect(store.categories.find((category) => category.id === 1)?.colorCode).toBe('#6366f1');
  });

  it('deletes a category locally and stores smart delete result', async () => {
    mockedCategoryService.deleteCategory.mockResolvedValue({ action: 'ARCHIVED', affectedRecords: 3 });

    const store = useCategoryStore();
    store.$patch({ categories: initialCategories });

    const result = await store.deleteCategory(1);

    expect(mockedCategoryService.deleteCategory).toHaveBeenCalledWith(1);
    expect(result).toEqual({ action: 'ARCHIVED', affectedRecords: 3 });
    expect(store.lastDeleteResult).toEqual({ action: 'ARCHIVED', affectedRecords: 3 });
    expect(store.categories.map((category) => category.id)).toEqual([2]);
  });
});
