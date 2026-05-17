<template>
  <div class="grid-card">
    <p v-if="loading" class="loading">正在加载时间格...</p>
    <p class="hint">提示：点击单个格子编辑；先点击起点，再按住 Shift 点击终点，可连续填充多个格子。</p>
    <div class="time-grid" aria-label="96 slot time grid">
      <TimeSlotCell
        v-for="slotIndex in 96"
        :key="slotIndex - 1"
        :slot-index="slotIndex - 1"
        :slot="slotsByIndex.get(slotIndex - 1)"
        :selected="selectedSlotIndexes.includes(slotIndex - 1)"
        @select="handleSelect"
        @delete="$emit('deleteSlot', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import TimeSlotCell from '@/components/TimeSlotCell.vue';
import { slotIndexRange } from '@/composables/useTimeSlots';
import type { TimeSlotDto } from '@/types/timeSlot';

interface TimeGridProps {
  slots: TimeSlotDto[];
  loading?: boolean;
}

const props = withDefaults(defineProps<TimeGridProps>(), { loading: false });
const emit = defineEmits<{
  (e: 'selectSlot', slotIndex: number): void;
  (e: 'selectSlotRange', slotIndexes: number[]): void;
  (e: 'deleteSlot', slotId: number): void;
}>();

const anchorSlotIndex = ref<number | null>(null);
const selectedSlotIndexes = ref<number[]>([]);
const slotsByIndex = computed(() => new Map(props.slots.map((slot) => [slot.slotIndex, slot])));

function handleSelect(slotIndex: number, event: MouseEvent) {
  if (event.shiftKey && anchorSlotIndex.value !== null) {
    selectedSlotIndexes.value = slotIndexRange(anchorSlotIndex.value, slotIndex);
    emit('selectSlotRange', selectedSlotIndexes.value);
    return;
  }

  anchorSlotIndex.value = slotIndex;
  selectedSlotIndexes.value = [slotIndex];
  emit('selectSlot', slotIndex);
}
</script>

<style scoped>
.grid-card { background: white; border-radius: 20px; padding: 20px; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06); }
.loading { margin: 0 0 12px; color: #4f46e5; }
.hint { margin: 0 0 12px; color: #6b7280; font-size: 14px; }
.time-grid { display: grid; grid-template-columns: repeat(8, minmax(0, 1fr)); gap: 10px; }
@media (max-width: 1024px) { .time-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); } }
@media (max-width: 640px) { .time-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
