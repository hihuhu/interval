export interface TimeSlotDto {
  id: number;
  date: string;
  slotIndex: number;
  activityName: string;
  categoryId: number | null;
  categoryName: string;
  categoryColor: string | null;
  categoryStatus: 'ACTIVE' | 'ARCHIVED' | null;
  categoryDisplayName: string;
}

export interface UpsertTimeSlotRequest {
  date: string;
  slotIndex: number;
  activityName: string;
  categoryId: number;
}

export interface DeleteTimeSlotResponseDto {
  deleted: boolean;
  slotId: number;
}
