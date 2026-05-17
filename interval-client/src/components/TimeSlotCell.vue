<template>
  <button
    type="button"
    data-testid="time-slot-cell"
    class="slot-cell"
    :class="{ occupied: Boolean(slot), selected }"
    :style="slot?.categoryColor ? { borderLeftColor: slot.categoryColor } : undefined"
    @click="$emit('select', slotIndex, $event)"
  >
    <span class="time">{{ slotIndexToRange(slotIndex) }}</span>
    <span v-if="slot" class="activity">{{ slot.activityName }}</span>
    <span v-if="slot" class="category">{{ slot.categoryDisplayName }}</span>
    <button v-if="slot" type="button" class="delete" @click.stop="$emit('delete', slot.id)">删除</button>
  </button>
</template>

<script setup lang="ts">
import { slotIndexToRange } from '@/composables/useTimeSlots';
import type { TimeSlotDto } from '@/types/timeSlot';

interface TimeSlotCellProps {
  slotIndex: number;
  slot?: TimeSlotDto;
  selected?: boolean;
}

withDefaults(defineProps<TimeSlotCellProps>(), { selected: false });
defineEmits<{
  (e: 'select', slotIndex: number, event: MouseEvent): void;
  (e: 'delete', slotId: number): void;
}>();
</script>

<style scoped>
.slot-cell { min-height: 86px; border: 1px solid #e5e7eb; border-left: 5px solid transparent; border-radius: 12px; background: #fff; padding: 8px; text-align: left; cursor: pointer; display: flex; flex-direction: column; gap: 4px; }
.slot-cell:hover { border-color: #a5b4fc; box-shadow: 0 6px 18px rgba(99, 102, 241, 0.12); }
.occupied { background: #f8fbff; }
.selected { border-color: #4f46e5; background: #eef2ff; box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.16); }
.time { font-size: 12px; color: #6b7280; }
.activity { font-weight: 700; color: #111827; }
.category { font-size: 12px; color: #4b5563; }
.delete { margin-top: auto; align-self: flex-start; border: 0; color: #dc2626; background: transparent; padding: 0; cursor: pointer; }
</style>
