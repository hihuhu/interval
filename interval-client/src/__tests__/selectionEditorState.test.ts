import { describe, expect, it } from 'vitest';
import { deriveSelectionEditorState } from '@/composables/useSelectionEditorState';
import type { TimeSlotDto } from '@/types/timeSlot';

function slot(slotIndex: number, categoryId: number, note: string | null): TimeSlotDto {
  return {
    id: slotIndex,
    date: '2026-05-26',
    slotIndex,
    activityName: 'Focus',
    note,
    categoryId,
    categoryName: categoryId === 1 ? 'Work' : 'Study',
    categoryColor: categoryId === 1 ? '#3b82f6' : '#22c55e',
    categoryStatus: 'ACTIVE',
    categoryDisplayName: categoryId === 1 ? 'Work' : 'Study',
  };
}

describe('deriveSelectionEditorState', () => {
  it('preselects a single category and shared note', () => {
    const state = deriveSelectionEditorState([10, 11], [slot(10, 1, 'Planning'), slot(11, 1, 'Planning')]);

    expect(state.categoryMode).toBe('single');
    expect(state.selectedCategoryId).toBe(1);
    expect(state.noteMode).toBe('single');
    expect(state.displayedNote).toBe('Planning');
    expect(state.overlapCount).toBe(2);
  });

  it('detects mixed categories and mixed notes without reusing stale editor values', () => {
    const state = deriveSelectionEditorState([10, 11], [slot(10, 1, 'Planning'), slot(11, 2, 'Reading')]);

    expect(state.categoryMode).toBe('mixed');
    expect(state.selectedCategoryId).toBeNull();
    expect(state.noteMode).toBe('mixed');
    expect(state.displayedNote).toBe('');
  });

  it('treats empty selected slots as no existing category or note', () => {
    const state = deriveSelectionEditorState([20, 21], []);

    expect(state.categoryMode).toBe('empty');
    expect(state.noteMode).toBe('empty');
    expect(state.hasRecordedSlot).toBe(false);
    expect(state.overlapCount).toBe(0);
  });
});
