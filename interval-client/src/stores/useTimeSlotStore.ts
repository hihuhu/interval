import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import * as timeSlotService from '@/services/timeSlotService';
import type { TimeSlotDto, UpsertTimeSlotRequest } from '@/types/timeSlot';
import { todayIsoDate } from '@/composables/useTimeSlots';

export const useTimeSlotStore = defineStore('timeSlot', () => {
  const selectedDate = ref(todayIsoDate());
  const slots = ref<TimeSlotDto[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref<string | null>(null);
  const slotsByIndex = computed(() => new Map(slots.value.map((slot) => [slot.slotIndex, slot])));

  function setDate(date: string) {
    selectedDate.value = date;
  }

  async function fetchDailySlots(date = selectedDate.value) {
    loading.value = true;
    errorMessage.value = null;
    try {
      slots.value = await timeSlotService.getDailySlots(date);
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load time slots';
      throw error;
    } finally {
      loading.value = false;
    }
  }

  async function upsertSlot(payload: UpsertTimeSlotRequest) {
    saving.value = true;
    try {
      return await saveSlot(payload);
    } finally {
      saving.value = false;
    }
  }

  async function upsertSlotRange(date: string, slotIndexes: number[], activityName: string, categoryId: number) {
    if (slotIndexes.length === 0) {
      return [];
    }

    saving.value = true;
    try {
      const savedSlots: TimeSlotDto[] = [];
      for (const slotIndex of slotIndexes) {
        savedSlots.push(await saveSlot({ date, slotIndex, activityName, categoryId }));
      }
      return savedSlots;
    } finally {
      saving.value = false;
    }
  }

  async function saveSlot(payload: UpsertTimeSlotRequest) {
    const saved = await timeSlotService.upsertTimeSlot(payload);
    slots.value = [...slots.value.filter((slot) => slot.id !== saved.id && slot.slotIndex !== saved.slotIndex), saved]
      .sort((a, b) => a.slotIndex - b.slotIndex);
    return saved;
  }

  async function deleteSlot(slotId: number) {
    const result = await timeSlotService.deleteTimeSlot(slotId);
    if (result.deleted) {
      slots.value = slots.value.filter((slot) => slot.id !== slotId);
    }
    return result;
  }

  return {
    selectedDate,
    slots,
    loading,
    saving,
    errorMessage,
    slotsByIndex,
    setDate,
    fetchDailySlots,
    upsertSlot,
    upsertSlotRange,
    deleteSlot,
  };
});
