export interface CategoryDto {
  id: number;
  name: string;
  colorCode: string;
  status: 'ACTIVE' | 'ARCHIVED';
  displayOrder: number;
}

export interface CreateCategoryRequest {
  name: string;
  colorCode: string;
}

export interface UpdateCategoryRequest {
  name: string;
  colorCode: string;
  displayOrder: number;
}

export interface DeleteCategoryResponseDto {
  action: 'DELETED' | 'ARCHIVED';
  affectedRecords: number;
}
