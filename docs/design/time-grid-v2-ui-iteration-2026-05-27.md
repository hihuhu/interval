# Time Grid v2 UI Iteration Notes

Date: 2026-05-27

This document records the latest UI iteration for the formal Vue implementation of `modern-time-grid-v2.html`. Statistics work remains paused; this note only covers the Time Grid workstation.

## Scope

- Refine the middle time-slot grid to better match the prototype.
- Rework selected and drag-preview states after browser review.
- Replace the right-side category native dropdown with the prototype-style category list.
- Keep existing click, toggle, drag-select, batch edit, and erase behavior intact.

## Time Slot Cell

Final design:

- Each cell is fixed at `40px` height.
- Empty cells render no visible text.
- Occupied cells render only a compact centered category label.
- Activity name, time range, category, and note are kept in `aria-label` / `title` instead of being shown inside the cell.
- Visible delete actions are not rendered inside grid cells. Destructive actions stay in the right-side editor panel.

Rejected designs:

- Card-like cell with time range, activity, category, note, and delete button.
- Tall `76px` cell layout.
- Per-cell visible delete button.

Rationale:

The 96-cell grid is primarily a scanning surface. Putting detailed content in every cell makes the grid noisy and moves it away from the prototype's clean rhythm.

## Selection Visual State

Final design:

- Selected cells use a light indigo fill, a clear `2px` inset outline, and a subtle shadow.
- Drag preview uses a lighter sky-blue treatment so it is distinguishable from committed selection.
- No bottom underline, no corner marker, and no heavy purple overlay.

Rejected designs:

- Heavy purple overlay: too dark and visually dirty.
- Very pale selected fill: too hard to see.
- Top-right dot marker: too decorative and visually awkward.
- Thin bottom underline: still felt unnecessary and unlike the desired grid surface.

Future tuning rule:

If the selected state still needs adjustment, tune only these three variables first:

- Inset outline opacity.
- Light fill opacity.
- Shadow strength.

Do not add new markers or decorative ornaments unless the prototype direction changes.

## Category Picker

Final design:

- The right-side editor no longer uses a native `<select>`.
- Categories render as a prototype-style list with color dot, category name, hover state, and selected state.
- The control uses `role="radiogroup"` and `role="radio"` for single-selection semantics.
- Long category lists scroll inside the editor panel.

Rationale:

The prototype exposes categories as a visible list, not as a dropdown. A list is faster to scan, keeps color cues visible, and feels consistent with the workstation layout.

## Verification

Commands run:

```bash
npm run test -- src/__tests__/SelectionEditorPanel.test.ts src/__tests__/TimeGrid.test.ts src/__tests__/slotSelection.test.ts
npm run build
```

Result:

- 3 frontend test files passed.
- 15 tests passed.
- Production build passed.

Browser checks:

- `/time-grid` loads successfully.
- Selecting a time cell opens the right-side editor.
- The editor shows category list items.
- `select[name="categoryId"]` is no longer present.
- Selected time cells no longer render the rejected bottom underline.

## Updated Files

- `interval-client/src/components/TimeSlotCell.vue`
- `interval-client/src/components/SelectionEditorPanel.vue`
- `interval-client/src/__tests__/TimeGrid.test.ts`
- `interval-client/src/__tests__/SelectionEditorPanel.test.ts`

## Next Steps

- Let the user review the current selected-state balance in the browser.
- Run a full browser smoke test for save, overwrite, mixed selection, note preservation, and erase behavior.
- Return to category-duration statistics only after the Time Grid main workflow is stable.
