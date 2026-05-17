import { http } from '@/services/http';
import type { DeleteTimeSlotResponseDto, TimeSlotDto, UpsertTimeSlotRequest } from '@/types/timeSlot';

export async function getDailySlots(date: string): Promise<TimeSlotDto[]> {
  return http.get<never, TimeSlotDto[]>('/api/time-slots', { params: { date } });
}

export async function upsertTimeSlot(payload: UpsertTimeSlotRequest): Promise<TimeSlotDto> {
  return http.put<never, TimeSlotDto>('/api/time-slots', payload);
}

export async function deleteTimeSlot(slotId: number): Promise<DeleteTimeSlotResponseDto> {
  return http.delete<never, DeleteTimeSlotResponseDto>(`/api/time-slots/${slotId}`);
}
