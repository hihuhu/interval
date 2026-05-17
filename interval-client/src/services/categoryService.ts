import { http } from '@/services/http';
import type { CategoryDto, CreateCategoryRequest, DeleteCategoryResponseDto, UpdateCategoryRequest } from '@/types/category';

export async function getCategories(): Promise<CategoryDto[]> {
  return http.get<never, CategoryDto[]>('/api/categories');
}

export async function createCategory(payload: CreateCategoryRequest): Promise<CategoryDto> {
  return http.post<never, CategoryDto>('/api/categories', payload);
}

export async function updateCategory(categoryId: number, payload: UpdateCategoryRequest): Promise<CategoryDto> {
  return http.put<never, CategoryDto>(`/api/categories/${categoryId}`, payload);
}

export async function deleteCategory(categoryId: number): Promise<DeleteCategoryResponseDto> {
  return http.delete<never, DeleteCategoryResponseDto>(`/api/categories/${categoryId}`);
}
