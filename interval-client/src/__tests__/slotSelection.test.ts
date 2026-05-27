import { describe, expect, it } from 'vitest';
import { useSlotSelection } from '@/composables/useSlotSelection';

describe('useSlotSelection', () => {
  it('toggles non-contiguous clicked slots', () => {
    const selection = useSlotSelection();

    selection.clickSlot(36);
    selection.clickSlot(44);
    selection.clickSlot(36);

    expect(selection.selectedSlotIndexes.value).toEqual([44]);
  });

  it('starts a drag selection by clearing the previous clicked selection', () => {
    const selection = useSlotSelection();

    selection.clickSlot(12);
    selection.clickSlot(18);
    selection.beginDrag(40);
    selection.moveDrag(43);
    selection.endDrag();

    expect(selection.selectedSlotIndexes.value).toEqual([40, 41, 42, 43]);
  });

  it('allows clicked additions after a drag range', () => {
    const selection = useSlotSelection();

    selection.beginDrag(8);
    selection.moveDrag(10);
    selection.endDrag();
    selection.clickSlot(32);

    expect(selection.selectedSlotIndexes.value).toEqual([8, 9, 10, 32]);
  });
});
