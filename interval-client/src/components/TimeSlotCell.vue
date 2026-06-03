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
  return uniqueText([
    timeRange.value,
    props.slot.categoryDisplayName || props.slot.categoryName,
    props.slot.activityName,
    props.slot.note,
  ]).join(' / ');
});
const ariaLabel = computed(() => {
  if (!props.slot) return `${timeRange.value} empty`;
  return uniqueText([
    timeRange.value,
    props.slot.categoryDisplayName || props.slot.categoryName,
    props.slot.activityName,
  ]).join(' ');
});

const cellStyle = computed(() => {
  if (!props.slot?.categoryColor) return undefined;
  return {
    '--slot-color': props.slot.categoryColor,
    '--slot-text-color': readableColor(props.slot.categoryColor),
    '--slot-surface': `${props.slot.categoryColor}26`,
    '--slot-surface-strong': `${props.slot.categoryColor}40`,
    '--slot-border': `${props.slot.categoryColor}66`,
  };
});

function readableColor(color: string) {
  const normalized = color.replace('#', '');
  if (!/^[0-9a-fA-F]{6}$/.test(normalized)) return '#1e293b';

  const red = Number.parseInt(normalized.slice(0, 2), 16);
  const green = Number.parseInt(normalized.slice(2, 4), 16);
  const blue = Number.parseInt(normalized.slice(4, 6), 16);

  if (green > red + blue && green > 150) return '#14532d';
  if (red > 200 && green > 150 && blue < 130) return '#7c2d12';
  if (red > 180 && blue > 150) return '#581c87';
  if (blue > red && blue > green) return '#1e3a8a';
  return '#334155';
}

function uniqueText(values: Array<string | null | undefined>) {
  const seen = new Set<string>();
  return values.flatMap((value) => {
    const text = value?.trim();
    if (!text || seen.has(text)) return [];
    seen.add(text);
    return [text];
  });
}
</script>

<style scoped>
.slot-cell {
  --empty-slot-surface: rgba(255, 255, 255, 0.46);
  --empty-slot-surface-hover: rgba(255, 255, 255, 0.72);
  position: relative;
  height: 42px;
  min-height: 42px;
  border: 1px solid rgba(148, 163, 184, 0.20);
  border-radius: 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.55), rgba(255, 255, 255, 0.18)),
    var(--empty-slot-surface);
  padding: 0 8px;
  text-align: center;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  user-select: none;
  touch-action: none;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.52);
  transition: background-color .14s ease, border-color .14s ease, box-shadow .14s ease, color .14s ease;
}

.slot-cell::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 11px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.32), transparent 58%);
  opacity: 0.82;
  pointer-events: none;
}

.slot-cell:hover {
  border-color: rgba(99, 102, 241, 0.35);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.60), rgba(255, 255, 255, 0.22)),
    var(--empty-slot-surface-hover);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.42),
    inset 0 0 0 2px rgba(99, 102, 241, 0.08);
}

.occupied {
  border-color: var(--slot-border, rgba(99, 102, 241, 0.45));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.50), rgba(255, 255, 255, 0.10)),
    linear-gradient(90deg, var(--slot-surface-strong, #c7d2fe) 0 5px, transparent 5px),
    var(--slot-surface, #eef2ff);
  color: var(--slot-text-color, #1e293b);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.30),
    0 1px 2px rgba(15, 23, 42, 0.025);
}

.selected {
  position: relative;
  border-color: rgba(79, 70, 229, 0.70);
  background:
    linear-gradient(180deg, rgba(238, 242, 255, 0.86), rgba(224, 231, 255, 0.80)),
    #e0e7ff;
  box-shadow:
    0 0 0 2px rgba(79, 70, 229, 0.42) inset,
    0 1px 0 rgba(255, 255, 255, 0.74) inset;
  color: #312e81;
}

.slot-cell.selected:hover {
  border-color: rgba(67, 56, 202, 0.86);
  background:
    linear-gradient(180deg, rgba(238, 242, 255, 0.94), rgba(224, 231, 255, 0.88)),
    #e0e7ff;
  box-shadow:
    0 0 0 2px rgba(67, 56, 202, 0.56) inset,
    0 0 0 1px rgba(67, 56, 202, 0.18);
  color: #312e81;
}

.preview {
  position: relative;
  border-color: rgba(14, 165, 233, 0.48);
  background:
    linear-gradient(180deg, rgba(240, 249, 255, 0.86), rgba(224, 242, 254, 0.72)),
    #e0f2fe;
  box-shadow:
    0 0 0 2px rgba(14, 165, 233, 0.20) inset,
    0 0 0 1px rgba(14, 165, 233, 0.10);
}

.preview::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 11px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(125, 211, 252, 0.18));
  pointer-events: none;
}

.slot-cell > * {
  position: relative;
  z-index: 2;
}

.slot-label {
  width: 100%;
  color: currentColor;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.05;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.46);
}
</style>
