import type { TimeSlotDto } from '@/types/timeSlot';

export type SelectionCategoryMode = 'empty' | 'single' | 'mixed';
export type SelectionNoteMode = 'empty' | 'single' | 'mixed';

export interface SelectionEditorState {
  categoryMode: SelectionCategoryMode;
  noteMode: SelectionNoteMode;
  selectedCategoryId: number | null;
  displayedNote: string;
  hasRecordedSlot: boolean;
  overlapCount: number;
}

export function deriveSelectionEditorState(selectedSlotIndexes: number[], slots: TimeSlotDto[]): SelectionEditorState {
  const slotsByIndex = new Map(slots.map((slot) => [slot.slotIndex, slot]));
  const selectedSlots = selectedSlotIndexes
    .map((slotIndex) => slotsByIndex.get(slotIndex))
    .filter((slot): slot is TimeSlotDto => Boolean(slot));
  const categoryIds = [...new Set(selectedSlots
    .map((slot) => slot.categoryId)
    .filter((categoryId): categoryId is number => categoryId !== null))];
  const notes = [...new Set(selectedSlotIndexes.map((slotIndex) => slotsByIndex.get(slotIndex)?.note?.trim() ?? ''))];

  return {
    categoryMode: resolveCategoryMode(categoryIds),
    noteMode: resolveNoteMode(notes),
    selectedCategoryId: categoryIds.length === 1 ? categoryIds[0] : null,
    displayedNote: notes.length === 1 ? notes[0] : '',
    hasRecordedSlot: selectedSlots.length > 0,
    overlapCount: selectedSlots.length,
  };
}

function resolveCategoryMode(categoryIds: number[]): SelectionCategoryMode {
  if (categoryIds.length === 0) return 'empty';
  if (categoryIds.length === 1) return 'single';
  return 'mixed';
}

function resolveNoteMode(notes: string[]): SelectionNoteMode {
  if (notes.length === 0) return 'empty';
  if (notes.length === 1) return notes[0] ? 'single' : 'empty';
  return 'mixed';
}
