import { ref } from 'vue';
import { defineStore } from 'pinia';
import * as categoryService from '@/services/categoryService';
import type { CategoryDto, CreateCategoryRequest, DeleteCategoryResponseDto, UpdateCategoryRequest } from '@/types/category';

function sortByDisplayOrder(categories: CategoryDto[]) {
  return [...categories].sort((a, b) => a.displayOrder - b.displayOrder);
}

export const useCategoryStore = defineStore('category', () => {
  const categories = ref<CategoryDto[]>([]);
  const loading = ref(false);
  const errorMessage = ref<string | null>(null);
  const lastDeleteResult = ref<DeleteCategoryResponseDto | null>(null);

  async function fetchCategories() {
    loading.value = true;
    errorMessage.value = null;
    try {
      categories.value = await categoryService.getCategories();
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load categories';
      throw error;
    } finally {
      loading.value = false;
    }
  }

  async function createCategory(payload: CreateCategoryRequest) {
    const category = await categoryService.createCategory(payload);
    categories.value = sortByDisplayOrder([...categories.value, category]);
    return category;
  }

  async function updateCategory(categoryId: number, payload: UpdateCategoryRequest) {
    const updatedCategory = await categoryService.updateCategory(categoryId, payload);
    categories.value = sortByDisplayOrder(
      categories.value.map((category) => category.id === categoryId ? updatedCategory : category),
    );
    return updatedCategory;
  }

  async function deleteCategory(categoryId: number) {
    const result = await categoryService.deleteCategory(categoryId);
    categories.value = categories.value.filter((category) => category.id !== categoryId);
    lastDeleteResult.value = result;
    return result;
  }

  return {
    categories,
    loading,
    errorMessage,
    lastDeleteResult,
    fetchCategories,
    createCategory,
    updateCategory,
    deleteCategory,
  };
});
