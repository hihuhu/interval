<template>
  <div class="grid-card">
    <div class="grid-head">
      <div>
        <h2>时间网格</h2>
        <p v-if="loading" class="status">正在加载时间格...</p>
        <p v-else class="status">纵向看时间节奏，横向看每小时的细分状态。</p>
      </div>
      <div class="grid-actions" aria-label="时间网格操作提示">
        <span>拖拽：连续范围</span>
        <span>单击：补选 / 取消</span>
      </div>
    </div>

    <div
      class="time-board"
      aria-label="96 slot time grid"
      @pointermove="handleGridPointerMove"
      @mousemove="handleGridMouseMove"
      @pointerleave="handlePointerLeave"
      @mouseleave="handleMouseLeave"
    >
      <div class="time-board-header" aria-hidden="true">
        <div></div>
        <div>:00</div>
        <div>:15</div>
        <div>:30</div>
        <div>:45</div>
      </div>
      <div class="time-board-scroll">
        <div v-for="hour in 24" :key="hour" class="time-row">
          <div class="hour-label">{{ hourLabel(hour - 1) }}</div>
          <TimeSlotCell
            v-for="quarter in 4"
            :key="slotIndexFor(hour, quarter)"
            :slot-index="slotIndexFor(hour, quarter)"
            :slot="slotsByIndex.get(slotIndexFor(hour, quarter))"
            :selected="selectedSlotIndexes.includes(slotIndexFor(hour, quarter))"
            :preview="dragPreviewSlotIndexes.includes(slotIndexFor(hour, quarter))"
            @select="handleSelect"
            @drag-start="handleDragStart"
            @drag-enter="handleDragEnter"
            @drag-end="handleDragEnd"
            @delete="$emit('deleteSlot', $event)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import TimeSlotCell from '@/components/TimeSlotCell.vue';
import { useSlotSelection } from '@/composables/useSlotSelection';
import type { TimeSlotDto } from '@/types/timeSlot';

interface TimeGridProps {
  slots: TimeSlotDto[];
  loading?: boolean;
}

const props = withDefaults(defineProps<TimeGridProps>(), { loading: false });
const emit = defineEmits<{
  (e: 'selectionChange', slotIndexes: number[]): void;
  (e: 'deleteSlot', slotId: number): void;
}>();

const {
  selectedSlotIndexes,
  dragPreviewSlotIndexes,
  clickSlot,
  beginDrag,
  moveDrag,
  endDrag,
} = useSlotSelection();
const slotsByIndex = computed(() => new Map(props.slots.map((slot) => [slot.slotIndex, slot])));

function handleSelect(slotIndex: number, event: MouseEvent) {
  clickSlot(slotIndex);
  emit('selectionChange', selectedSlotIndexes.value);
}

function handleDragStart(slotIndex: number, event: PointerEvent | MouseEvent) {
  if (event.button !== 0) return;
  beginDrag(slotIndex);
}

function handleDragEnter(slotIndex: number, event: PointerEvent | MouseEvent) {
  if (event.buttons !== 1 && event.buttons !== 0) return;
  moveDrag(slotIndex);
}

function handleDragEnd() {
  endDrag();
  emit('selectionChange', selectedSlotIndexes.value);
}

function handleGridPointerMove(event: PointerEvent) {
  if (event.buttons !== 1) return;
  moveDragToPoint(event.clientX, event.clientY);
}

function handleGridMouseMove(event: MouseEvent) {
  if (event.buttons !== 1) return;
  moveDragToPoint(event.clientX, event.clientY);
}

function handlePointerLeave(event: PointerEvent) {
  if (event.buttons !== 1) return;
  handleDragEnd();
}

function handleMouseLeave(event: MouseEvent) {
  if (event.buttons !== 1) return;
  handleDragEnd();
}

function moveDragToPoint(clientX: number, clientY: number) {
  const slotIndex = slotIndexFromPoint(clientX, clientY);
  if (slotIndex === null) return;
  moveDrag(slotIndex);
}

function slotIndexFromPoint(clientX: number, clientY: number): number | null {
  const element = document.elementFromPoint(clientX, clientY)?.closest('[data-slot-index]');
  const rawSlotIndex = element?.getAttribute('data-slot-index');
  if (!rawSlotIndex) return null;

  const slotIndex = Number(rawSlotIndex);
  return Number.isInteger(slotIndex) && slotIndex >= 0 && slotIndex < 96 ? slotIndex : null;
}

function slotIndexFor(hour: number, quarter: number): number {
  return (hour - 1) * 4 + (quarter - 1);
}

function hourLabel(hour: number): string {
  return `${hour.toString().padStart(2, '0')}:00`;
}
</script>

<style scoped>
.grid-card {
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.9), rgba(255, 255, 255, 0.96)),
    linear-gradient(90deg, rgba(99, 102, 241, 0.03), transparent 40%);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 24px;
  padding: 18px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.07);
}

.grid-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

h2 {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 19px;
}

.status {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.grid-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.grid-actions span {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #475569;
  font-size: 12px;
  font-weight: 800;
}

.time-board {
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.88), rgba(255, 255, 255, 0.96)),
    linear-gradient(90deg, rgba(99, 102, 241, 0.03), transparent 40%);
  padding: 16px;
}

.time-board-header,
.time-row {
  display: grid;
  grid-template-columns: 52px repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.time-board-header {
  margin-bottom: 12px;
}

.time-board-header div {
  color: #cbd5e1;
  font-size: 10px;
  font-weight: 700;
  text-align: center;
}

.time-board-scroll {
  max-height: 640px;
  overflow-y: auto;
  padding-right: 4px;
}

.time-row {
  margin-bottom: 6px;
}

.hour-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 12px;
  color: #cbd5e1;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}

@media (max-width: 780px) {
  .time-board {
    padding: 12px;
  }

  .time-board-header,
  .time-row {
    grid-template-columns: 44px repeat(4, minmax(56px, 1fr));
  }

  .time-board-scroll {
    overflow-x: auto;
  }
}

@media (max-width: 520px) {
  .grid-head {
    flex-direction: column;
  }

  .grid-actions {
    justify-content: flex-start;
  }
}
</style>
