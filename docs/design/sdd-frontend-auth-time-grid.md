# 前端认证与 Time Grid 基础架构 SDD

## 1. Overview

本设计用于初始化 `interval-client` Vue 3 前端，并实现与当前 Spring Boot 后端对接的第一条完整闭环：注册、登录、JWT 保存、路由守卫、分类查询/新建、TimeSlot 查询/创建/删除，以及一天 96 个 15 分钟格子的基础 Time Grid 页面。

当前后端已经完成 JWT 用户上下文集成：Category 与 TimeSlot API 使用 `Authorization: Bearer <token>`，不再读取 `X-User-Id`。

本阶段目标是建立稳定、可测试、可扩展的前端基础。第一版已经覆盖单格 Time Grid 闭环；下一阶段补齐分类编辑/智能删除 UI，并在此基础上增强连续多格填充体验。拖拽多选、批量保存、统计分析、refresh token 按独立设计逐步推进。

## 2. Technology Stack

- Vue 3 Composition API + `<script setup lang="ts">`
- TypeScript strict mode
- Vite
- Vue Router 4
- Pinia setup stores
- Axios
- Vitest + Vue Test Utils + jsdom
- Scoped CSS

## 3. Project Structure

```text
interval-client/
├── package.json
├── index.html
├── vite.config.ts
├── tsconfig.json
├── vitest.config.ts
├── .env.example
└── src/
    ├── main.ts
    ├── App.vue
    ├── router/index.ts
    ├── stores/
    │   ├── useAuthStore.ts
    │   ├── useCategoryStore.ts
    │   └── useTimeSlotStore.ts
    ├── services/
    │   ├── http.ts
    │   ├── authService.ts
    │   ├── categoryService.ts
    │   └── timeSlotService.ts
    ├── types/
    │   ├── api.ts
    │   ├── auth.ts
    │   ├── category.ts
    │   └── timeSlot.ts
    ├── components/
    │   ├── AppLayout.vue
    │   ├── TimeGrid.vue
    │   ├── TimeSlotCell.vue
    │   ├── CategorySelect.vue
    │   └── SlotEditorModal.vue
    ├── views/
    │   ├── LoginView.vue
    │   ├── RegisterView.vue
    │   ├── TimeGridView.vue
    │   └── NotFoundView.vue
    └── test/setup.ts
```

## 4. API Integration

All backend responses use:

```typescript
export interface ApiResponse<T> {
  result: 'SUCCESS' | 'ERROR';
  message: string;
  data: T | null;
}
```

The Axios service layer unwraps `data` on success and throws a typed error on failure. API base URL comes from `import.meta.env.VITE_API_BASE_URL`; it must not be hardcoded.

### Auth

- `POST /api/auth/register`
  - request: `{ username: string; password: string }`
  - data: `{ userId: number; username: string }`
- `POST /api/auth/login`
  - request: `{ username: string; password: string }`
  - data: `{ token: string; username: string }`

### Category

All require Bearer token.

- `GET /api/categories`
  - data: `CategoryDto[]`
- `POST /api/categories`
  - request: `{ name: string; colorCode: string }`
  - data: `CategoryDto`

```typescript
export interface CategoryDto {
  id: number;
  name: string;
  colorCode: string;
  status: 'ACTIVE' | 'ARCHIVED';
  displayOrder: number;
}
```

### TimeSlot

All require Bearer token.

- `GET /api/time-slots?date=YYYY-MM-DD`
  - data: `TimeSlotDto[]`
- `PUT /api/time-slots`
  - request: `{ date: string; slotIndex: number; activityName: string; categoryId: number }`
  - data: `TimeSlotDto`
- `DELETE /api/time-slots/{slotId}`
  - data: `{ deleted: boolean; slotId: number }`

```typescript
export interface TimeSlotDto {
  id: number;
  date: string;
  slotIndex: number;
  activityName: string;
  categoryId: number | null;
  categoryName: string;
  categoryColor: string | null;
  categoryStatus: 'ACTIVE' | 'ARCHIVED' | null;
  categoryDisplayName: string;
}
```

## 5. Component Architecture

```text
App.vue
└── RouterView
    ├── LoginView
    ├── RegisterView
    └── TimeGridView
        └── AppLayout
            ├── date selector
            ├── category quick-create form
            ├── TimeGrid
            │   └── TimeSlotCell × 96
            └── SlotEditorModal
                └── CategorySelect
```

### AppLayout.vue

Props:

```typescript
interface AppLayoutProps { username: string }
```

Emits:

```typescript
{ (e: 'logout'): void }
```

### TimeGrid.vue

Props:

```typescript
interface TimeGridProps {
  slots: TimeSlotDto[];
  loading?: boolean;
}
```

Emits:

```typescript
{
  (e: 'selectSlot', slotIndex: number): void;
  (e: 'deleteSlot', slotId: number): void;
}
```

### TimeSlotCell.vue

Props:

```typescript
interface TimeSlotCellProps {
  slotIndex: number;
  slot?: TimeSlotDto;
}
```

Emits:

```typescript
{
  (e: 'select', slotIndex: number): void;
  (e: 'delete', slotId: number): void;
}
```

### CategorySelect.vue

Props:

```typescript
interface CategorySelectProps {
  categories: CategoryDto[];
  modelValue: number | null;
  disabled?: boolean;
}
```

Emits:

```typescript
{ (e: 'update:modelValue', value: number | null): void }
```

### SlotEditorModal.vue

Props:

```typescript
interface SlotEditorModalProps {
  open: boolean;
  slotIndex: number | null;
  existingSlot?: TimeSlotDto;
  categories: CategoryDto[];
  saving?: boolean;
}
```

Emits:

```typescript
{
  (e: 'close'): void;
  (e: 'submit', payload: { slotIndex: number; activityName: string; categoryId: number }): void;
}
```

## 6. State Management

### useAuthStore

State: `token`, `username`, `loading`, `errorMessage`.

Getter: `isAuthenticated`.

Actions:

- `restoreSession()` reads localStorage.
- `login(username, password)` calls API and stores token/username.
- `register(username, password)` calls API.
- `logout()` clears store and localStorage.

Persistence keys:

- `interval.auth.token`
- `interval.auth.username`

### useCategoryStore

State: `categories`, `loading`, `errorMessage`.

Actions: `fetchCategories()`, `createCategory(payload)`.

### useTimeSlotStore

State: `selectedDate`, `slots`, `loading`, `saving`, `errorMessage`.

Getter: `slotsByIndex`.

Actions: `setDate(date)`, `fetchDailySlots(date)`, `upsertSlot(payload)`, `deleteSlot(slotId)`.

## 7. Data Flow

Login:

```text
LoginView -> useAuthStore.login -> authService.login -> backend token -> localStorage -> router /time-grid
```

Protected request:

```text
Store -> service -> Axios interceptor attaches Bearer token -> backend -> ApiResponse unwrap
```

Time Grid load:

```text
TimeGridView mounted -> fetchCategories + fetchDailySlots -> TimeGrid renders 96 cells
```

Save slot:

```text
Click cell -> SlotEditorModal -> submit -> timeSlotStore.upsertSlot -> update local slot list
```

## 8. Router Design

Routes:

| Name | Path | Auth |
| --- | --- | --- |
| `login` | `/login` | public |
| `register` | `/register` | public |
| `timeGrid` | `/time-grid` | protected |
| `homeRedirect` | `/` | redirect by auth |
| `notFound` | `/:pathMatch(.*)*` | public |

Guard rules:

- `meta.requiresAuth` without token redirects to `/login`.
- Authenticated user visiting `/login` or `/register` redirects to `/time-grid`.
- App startup restores auth state before route decisions.

## 9. UI/UX Considerations

- Auth pages follow existing prototype direction: clean card on soft gradient background.
- Time Grid page uses a card layout with header, date selector, quick category creation, 96-cell grid, and modal editor.
- Cells show time range, activity name, category display name, and category color accent.
- Empty cells have hover affordance and open create modal on click.
- Occupied cells can be edited by click and deleted with a small action button.
- Loading states disable submit buttons.
- API errors are displayed inline.
- On 401, auth state is cleared and user is redirected to `/login`.
- Forms use labels; buttons use text labels; UI does not rely on color alone.

## 10. Testing Strategy

Unit tests:

- Axios attaches Bearer token when present.
- Axios unwraps `ApiResponse.data` on success and throws on error.
- Auth store restores, logs in, and logs out correctly.
- Slot time helper maps `0 -> 00:00-00:15` and `95 -> 23:45-24:00`.

Component tests:

- `LoginView` renders fields, submits login, and displays errors.
- `TimeGrid` renders exactly 96 cells and emits `selectSlot`.
- `SlotEditorModal` validates activity/category and emits submit payload.

Router tests:

- Unauthenticated `/time-grid` redirects to `/login`.
- Authenticated `/login` redirects to `/time-grid`.

Manual verification with backend at `http://localhost:8088`:

1. Register user.
2. Login user.
3. Confirm token is saved.
4. Create category.
5. Create TimeSlot.
6. Refresh and confirm TimeSlot reloads.
7. Delete TimeSlot.
8. Logout and confirm protected route redirects.

## 11. Implementation Plan After Approval

1. Initialize Vite Vue project files in `interval-client`.
2. Add npm dependencies for Vue, Vite, Router, Pinia, Axios, Vitest, Vue Test Utils, jsdom.
3. Add env config and path alias.
4. Add type definitions.
5. Write tests for service/store/router/helpers/components.
6. Implement service layer and stores.
7. Implement router and guards.
8. Implement views/components.
9. Run `npm install`, `npm run test`, `npm run build`.
10. Manual HTTP verification with backend.

## 12. Open Questions

1. 是否批准我直接初始化完整 Vue/Vite 项目并安装 npm 依赖？
2. Time Grid 第一版是否接受“点击单格创建/编辑”，暂不做拖拽多选？
3. 分类管理第一版是否只做“查询 + 新建”，把编辑/智能删除弹窗放到下一阶段？

## 13. Acceptance Criteria

- `npm run dev` can start the frontend.
- `npm run test` passes.
- `npm run build` passes.
- Login/register pages call real backend APIs.
- Protected `/time-grid` requires JWT login.
- Axios attaches Bearer token automatically.
- Time Grid renders 96 slots.
- User can create at least one category.
- User can create, query, and delete at least one TimeSlot through UI.
- No frontend API call uses `X-User-Id`.
