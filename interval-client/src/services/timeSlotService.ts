import { http } from '@/services/http';
import type {
  BatchDeleteTimeSlotRequest,
  BatchDeleteTimeSlotResponseDto,
  BatchUpsertTimeSlotRequest,
  BatchUpsertTimeSlotResponseDto,
  DeleteTimeSlotResponseDto,
  TimeSlotDto,
  UpsertTimeSlotRequest,
} from '@/types/timeSlot';

export async function getDailySlots(date: string): Promise<TimeSlotDto[]> {
  return http.get<never, TimeSlotDto[]>('/api/time-slots', { params: { date } });
}

export async function upsertTimeSlot(payload: UpsertTimeSlotRequest): Promise<TimeSlotDto> {
  return http.put<never, TimeSlotDto>('/api/time-slots', payload);
}

export async function upsertTimeSlotBatch(payload: BatchUpsertTimeSlotRequest): Promise<BatchUpsertTimeSlotResponseDto> {
  return http.post<never, BatchUpsertTimeSlotResponseDto>('/api/time-slots/batch', payload);
}

export async function deleteTimeSlot(slotId: number): Promise<DeleteTimeSlotResponseDto> {
  return http.delete<never, DeleteTimeSlotResponseDto>(`/api/time-slots/${slotId}`);
}

export async function deleteTimeSlotBatch(payload: BatchDeleteTimeSlotRequest): Promise<BatchDeleteTimeSlotResponseDto> {
  return http.delete<never, BatchDeleteTimeSlotResponseDto>('/api/time-slots/batch', { data: payload });
}
