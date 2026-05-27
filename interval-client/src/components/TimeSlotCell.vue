<template>
  <button
    type="button"
    data-testid="time-slot-cell"
    :data-slot-index="slotIndex"
    class="slot-cell"
    :class="{ occupied: Boolean(slot), selected, preview }"
    :style="cellStyle"
    :aria-label="ariaLabel"
    :title="tooltipText"
    @click="$emit('select', slotIndex, $event)"
    @pointerdown="$emit('dragStart', slotIndex, $event)"
    @pointerenter="$emit('dragEnter', slotIndex, $event)"
    @pointerup="$emit('dragEnd', $event)"
    @mousedown="$emit('dragStart', slotIndex, $event)"
    @mouseenter="$emit('dragEnter', slotIndex, $event)"
    @mouseup="$emit('dragEnd', $event)"
  >
    <span v-if="slot" class="slot-label">{{ cellLabel }}</span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { slotIndexToRange } from '@/composables/useTimeSlots';
import type { TimeSlotDto } from '@/types/timeSlot';

interface TimeSlotCellProps {
  slotIndex: number;
  slot?: TimeSlotDto;
  selected?: boolean;
  preview?: boolean;
}

const props = withDefaults(defineProps<TimeSlotCellProps>(), { selected: false, preview: false });
defineEmits<{
  (e: 'select', slotIndex: number, event: MouseEvent): void;
  (e: 'dragStart', slotIndex: number, event: PointerEvent | MouseEvent): void;
  (e: 'dragEnter', slotIndex: number, event: PointerEvent | MouseEvent): void;
  (e: 'dragEnd', event: PointerEvent | MouseEvent): void;
  (e: 'delete', slotId: number): void;
}>();

const timeRange = computed(() => slotIndexToRange(props.slotIndex));
const cellLabel = computed(() => props.slot?.categoryDisplayName || props.slot?.categoryName || '');
const tooltipText = computed(() => {
  if (!props.slot) return timeRange.value;
  return [timeRange.value, props.slot.activityName, props.slot.categoryDisplayName, props.slot.note]
    .filter(Boolean)
    .join(' / ');
});
const ariaLabel = computed(() => {
  if (!props.slot) return `${timeRange.value} empty`;
  return `${timeRange.value} ${props.slot.activityName || props.slot.categoryDisplayName} ${props.slot.categoryDisplayName}`;
});

const cellStyle = computed(() => {
  if (!props.slot?.categoryColor) return undefined;
  return {
    '--slot-color': props.slot.categoryColor,
    backgroundColor: `${props.slot.categoryColor}24`,
    borderColor: `${props.slot.categoryColor}52`,
  };
});
</script>

<style scoped>
.slot-cell {
  position: relative;
  height: 40px;
  min-height: 40px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
  padding: 0 8px;
  text-align: center;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  user-select: none;
  touch-action: none;
  transition: transform .15s ease, box-shadow .15s ease, border-color .15s ease, background .15s ease;
}

.slot-cell:hover {
  transform: translateY(-1px);
  border-color: rgba(129, 140, 248, 0.75);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.22) inset, 0 10px 16px rgba(99, 102, 241, 0.08);
}

.occupied {
  color: var(--slot-color, #6366f1);
}

.selected {
  position: relative;
  border-color: rgba(79, 70, 229, 0.72);
  background:
    linear-gradient(180deg, rgba(238, 242, 255, 0.72), rgba(255, 255, 255, 0.92)),
    rgba(255, 255, 255, 0.92);
  box-shadow:
    0 0 0 2px rgba(79, 70, 229, 0.48) inset,
    0 1px 0 rgba(255, 255, 255, 0.9) inset,
    0 9px 15px rgba(79, 70, 229, 0.08);
  transform: translateY(-1px);
}

.preview {
  position: relative;
  border-color: rgba(96, 165, 250, 0.58);
  background:
    linear-gradient(180deg, rgba(239, 246, 255, 0.78), rgba(255, 255, 255, 0.88)),
    rgba(255, 255, 255, 0.88);
  box-shadow:
    0 0 0 2px rgba(96, 165, 250, 0.24) inset,
    0 8px 14px rgba(59, 130, 246, 0.07);
  transform: translateY(-1px);
}

.preview::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 11px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0), rgba(186, 230, 253, 0.26));
  pointer-events: none;
}

.slot-cell > * {
  position: relative;
  z-index: 2;
}

.slot-label {
  width: 100%;
  color: currentColor;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
