export interface TimeSlotDto {
  id: number;
  date: string;
  slotIndex: number;
  activityName: string;
  note: string | null;
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
  note?: string | null;
  categoryId: number;
}

export interface BatchTimeSlotItem {
  slotIndex: number;
  activityName: string;
  categoryId: number;
  note?: string | null;
  noteTouched?: boolean;
}

export interface BatchUpsertTimeSlotRequest {
  date: string;
  slots: BatchTimeSlotItem[];
}

export interface BatchUpsertTimeSlotResponseDto {
  savedCount: number;
  slots: TimeSlotDto[];
}

export interface DeleteTimeSlotResponseDto {
  deleted: boolean;
  slotId: number;
}

export interface BatchDeleteTimeSlotRequest {
  slotIds: number[];
}

export interface BatchDeleteTimeSlotResponseDto {
  deletedCount: number;
  slotIds: number[];
}
