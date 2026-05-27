import { computed, ref } from 'vue';
import { slotIndexRange } from '@/composables/useTimeSlots';

export function useSlotSelection() {
  const selectedSlotIndexes = ref<number[]>([]);
  const dragStartSlotIndex = ref<number | null>(null);
  const dragEndSlotIndex = ref<number | null>(null);
  const dragging = ref(false);
  const suppressClickSlotIndex = ref<number | null>(null);

  const dragPreviewSlotIndexes = computed(() => {
    if (!dragging.value || dragStartSlotIndex.value === null || dragEndSlotIndex.value === null) {
      return [];
    }
    return slotIndexRange(dragStartSlotIndex.value, dragEndSlotIndex.value);
  });

  function clickSlot(slotIndex: number) {
    if (suppressClickSlotIndex.value === slotIndex) {
      suppressClickSlotIndex.value = null;
      return;
    }

    suppressClickSlotIndex.value = null;

    if (selectedSlotIndexes.value.includes(slotIndex)) {
      selectedSlotIndexes.value = selectedSlotIndexes.value.filter((index) => index !== slotIndex);
      return;
    }

    selectedSlotIndexes.value = [...selectedSlotIndexes.value, slotIndex].sort((a, b) => a - b);
  }

  function beginDrag(slotIndex: number) {
    dragging.value = true;
    dragStartSlotIndex.value = slotIndex;
    dragEndSlotIndex.value = slotIndex;
  }

  function moveDrag(slotIndex: number) {
    if (!dragging.value) return;
    dragEndSlotIndex.value = slotIndex;
  }

  function endDrag() {
    if (!dragging.value || dragStartSlotIndex.value === null || dragEndSlotIndex.value === null) {
      resetDrag();
      return;
    }

    const range = slotIndexRange(dragStartSlotIndex.value, dragEndSlotIndex.value);
    if (range.length > 1) {
      selectedSlotIndexes.value = range;
      suppressClickSlotIndex.value = dragEndSlotIndex.value;
    }
    resetDrag();
  }

  function clearSelection() {
    selectedSlotIndexes.value = [];
    suppressClickSlotIndex.value = null;
    resetDrag();
  }

  function resetDrag() {
    dragging.value = false;
    dragStartSlotIndex.value = null;
    dragEndSlotIndex.value = null;
  }

  return {
    selectedSlotIndexes,
    dragging,
    dragPreviewSlotIndexes,
    clickSlot,
    beginDrag,
    moveDrag,
    endDrag,
    clearSelection,
  };
}
