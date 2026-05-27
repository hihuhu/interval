# 前端分类管理与 Time Grid 范围填充扩展 SDD

## 1. Overview

本扩展设计建立在 `sdd-frontend-auth-time-grid.md` 已实现的前端基础上，补齐两个剩余能力：

1. 分类管理从“查询 + 新建”扩展为“查询 + 新建 + 编辑 + 智能删除结果提示”。
2. Time Grid 从单格编辑扩展为轻量连续范围填充。

后端已有分类更新、分类智能删除、TimeSlot 单格 upsert/delete API。本阶段优先复用现有后端接口，不新增后端批量接口，降低联调和回归风险。

## 2. Component Architecture

当前仍以 `TimeGridView` 作为页面容器：

```text
TimeGridView
├── AppLayout
├── toolbar
│   ├── date selector
│   └── quick category create form
├── category management panel
│   └── category row × active categories
│       ├── name input
│       ├── color input
│       ├── save button
│       └── delete button
├── TimeGrid
│   └── TimeSlotCell × 96
└── SlotEditorModal
    └── CategorySelect
```

为保持迭代小而清晰，分类管理 UI 先内聚在 `TimeGridView` 中；如果后续分类管理继续扩展为排序、恢复归档、批量操作，再抽取 `CategoryManager.vue`。

## 3. Data Flow

### Category update

```text
User edits row -> TimeGridView validates non-blank name -> useCategoryStore.updateCategory -> categoryService.updateCategory -> PUT /api/categories/{id} -> replace local category
```

### Category smart delete

```text
User clicks delete -> confirm -> useCategoryStore.deleteCategory -> categoryService.deleteCategory -> DELETE /api/categories/{id} -> remove active row -> show DELETED/ARCHIVED notice
```

### Range fill

```text
Shift-click cell -> TimeGrid emits inclusive slot range -> SlotEditorModal displays range summary -> submit -> TimeGridView loops selected slot indexes -> useTimeSlotStore.upsertSlot for each index
```

## 4. API Integration Points

All endpoints require `Authorization: Bearer <token>` through the existing Axios interceptor.

### Update category

- Method: `PUT`
- Path: `/api/categories/{categoryId}`
- Request body:

```typescript
interface UpdateCategoryRequest {
  name: string;
  colorCode: string;
  displayOrder: number;
}
```

- Validation rules:
  - `categoryId` must belong to current authenticated user.
  - `name` must be non-blank after trimming.
  - `colorCode` must be a valid hex color accepted by the backend.
  - `displayOrder` is preserved from the current category in this UI.
- Success status: `200 OK`
- Success response data: `CategoryDto`
- Error responses:
  - `400` invalid input, duplicate active category name, or category not found for user.
  - `401` missing or invalid JWT.
  - `500` unexpected server error.

### Smart delete category

- Method: `DELETE`
- Path: `/api/categories/{categoryId}`
- Request body: none
- Success status: `200 OK`
- Success response data:

```typescript
interface DeleteCategoryResponseDto {
  action: 'DELETED' | 'ARCHIVED';
  affectedRecords: number;
}
```

- Behavior:
  - `DELETED`: category had no historical TimeSlot references and was physically removed.
  - `ARCHIVED`: category had historical TimeSlot references and was archived.
- Error responses:
  - `400` category not found for user.
  - `401` missing or invalid JWT.
  - `500` unexpected server error.

### Range fill TimeSlot

No backend API change in this iteration.

Frontend payload:

```typescript
interface SlotRangeSubmitPayload {
  slotIndexes: number[];
  activityName: string;
  categoryId: number;
}
```

The frontend submits one existing `PUT /api/time-slots` request per slot index:

```typescript
interface UpsertTimeSlotRequest {
  date: string;
  slotIndex: number;
  activityName: string;
  categoryId: number;
}
```

## 5. State Management Design

### useCategoryStore additions

State:

- `lastDeleteResult: DeleteCategoryResponseDto | null`

Actions:

- `updateCategory(categoryId, payload)`
  - calls service
  - replaces the matching local category
  - sorts by `displayOrder`
  - returns updated category
- `deleteCategory(categoryId)`
  - calls service
  - removes the category from the active local list for both `DELETED` and `ARCHIVED`
  - stores and returns `lastDeleteResult`

### useTimeSlotStore additions

Action:

- `upsertSlotRange(date, slotIndexes, activityName, categoryId)`
  - validates non-empty `slotIndexes`
  - sequentially calls existing `upsertSlot`
  - returns updated slots

Sequential calls are acceptable for the current 96-slot grid and avoid hidden partial-failure behavior. If range fill becomes heavy, a backend batch endpoint can be designed later.

## 6. UI/UX Considerations

- Category panel appears above the grid and below the toolbar.
- Each category row is compact and keyboard-accessible.
- Delete uses `window.confirm` in this iteration to prevent accidental deletion without introducing a modal dependency.
- Smart delete result is shown inline as a success notice.
- Error messages continue to use the existing page-level error area.
- Range selection uses Shift-click because it is simple, familiar, and testable.
- Selected range is visually highlighted in the grid.
- Modal title and range text distinguish single-slot editing from range fill.

## 7. Business Logic and Edge Cases

- Empty category names are rejected client-side before calling the API.
- Deleting a category currently selected in the editor should not break the editor; the next editor open will default to the first available active category.
- Archived categories are removed from the active dropdown because `GET /api/categories` returns active categories only.
- Existing TimeSlot DTOs can still show archived category metadata supplied by the backend.
- Range fill overwrites existing slots by design, matching current upsert semantics.
- Range indexes are normalized so Shift-click works in both directions.

## 8. Testing Strategy

### Category tests

- Service/store test verifies update request and local replacement.
- Service/store test verifies delete request, local removal, and `lastDeleteResult` update.
- Validation behavior rejects blank category names in the view-level handler.

### Time Grid range tests

- Helper test verifies inclusive range generation.
- `TimeGrid` component test verifies Shift-click emits a selected range.
- Store test verifies `upsertSlotRange` calls existing upsert logic for each slot index and updates local state.

## 9. Acceptance Criteria

- User can edit category name and color from the Time Grid page.
- User can delete an unused category and see a `DELETED` result.
- User can delete a used category and see an `ARCHIVED` result with affected record count.
- User can Shift-click a second cell to select a continuous slot range.
- User can fill multiple selected time slots with the same activity/category without a backend API change.
- `npm run test` passes.
- `npm run build` passes.
