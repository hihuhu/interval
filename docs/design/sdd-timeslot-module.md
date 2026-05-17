# TimeSlot 模块系统设计文档

## 1. Overview

TimeSlot 模块是 Interval 的核心记录模块，用于让已登录用户按固定 15 分钟粒度记录每天的活动。一天被拆分为 96 个格子，每个格子由 `date + slotIndex` 唯一标识。

本阶段范围：

- 查询某用户某天的时间格记录。
- 新建或覆盖某个时间格记录（Upsert）。
- 删除某个时间格记录。
- 查询时携带分类回显信息，包括已归档分类。
- 保证用户数据隔离，用户只能访问自己的时间格。

不在本阶段范围：

- 批量保存多个 TimeSlot。
- 周/月统计分析。
- JWT 用户上下文自动注入到所有 API。
- 前端 UI 实现。

## 2. Architecture

### 2.1 Layered Architecture

```text
REST Controller
  -> TimeSlotService interface
    -> TimeSlotServiceImpl
      -> TimeSlotRepository
      -> CategoryRepository
        -> H2 Database
```

### 2.2 Package Plan

```text
com.interval.timeslot
├── controller
│   └── TimeSlotController.java
├── dto
│   ├── TimeSlotDto.java
│   ├── UpsertTimeSlotRequest.java
│   └── DeleteTimeSlotResponseDto.java
├── entity
│   └── TimeSlot.java
├── repository
│   └── TimeSlotRepository.java
└── service
    ├── TimeSlotService.java
    └── TimeSlotServiceImpl.java
```

### 2.3 Data Flow

#### Query daily slots

```text
Client
  -> GET /api/time-slots?userId={userId}&date={date}
  -> TimeSlotController
  -> TimeSlotService.getDailySlots(userId, date)
  -> TimeSlotRepository.findByUserIdAndDateOrderBySlotIndexAsc(userId, date)
  -> TimeSlotDto list with category display fields
  -> ApiResponse<List<TimeSlotDto>>
```

#### Upsert one slot

```text
Client
  -> PUT /api/time-slots?userId={userId}
  -> TimeSlotController
  -> TimeSlotService.upsertTimeSlot(userId, request)
  -> validate slotIndex, activityName, category ownership/status
  -> find existing by userId + date + slotIndex
  -> update existing or create new entity
  -> save via TimeSlotRepository
  -> ApiResponse<TimeSlotDto>
```

#### Delete one slot

```text
Client
  -> DELETE /api/time-slots/{slotId}?userId={userId}
  -> TimeSlotController
  -> TimeSlotService.deleteTimeSlot(userId, slotId)
  -> find by id and userId
  -> delete entity
  -> ApiResponse<DeleteTimeSlotResponseDto>
```

## 3. Data Model

### 3.1 Existing Entity: TimeSlot

| Field | Type | Constraint | Description |
| --- | --- | --- | --- |
| `id` | Long | PK, auto generated | TimeSlot ID |
| `userId` | Long | not null | Owner user ID |
| `date` | LocalDate | not null | Calendar date |
| `slotIndex` | Integer | not null, 0-95 by service validation | 15-minute slot index |
| `activityName` | String | not null | Activity name |
| `category` | Category | not null, lazy many-to-one | Related category |
| `createdAt` | Instant | not null | Creation timestamp |
| `updatedAt` | Instant | not null | Last update timestamp |

### 3.2 Unique Constraint

The database must keep the existing unique constraint:

```text
UNIQUE(user_id, date, slot_index)
```

Reason: each user can have at most one record for one date and one slot index. Upsert is the expected write behavior when the same key already exists.

### 3.3 Category Relationship Rules

- Upsert requires `categoryId` to belong to the same `userId`.
- Upsert only allows categories whose status is `ACTIVE`.
- Query must still display historical records that reference `ARCHIVED` categories.
- If a category record is missing unexpectedly, the response should not expose a server error when avoidable; the category display fields may use fallback values.

## 4. API Contract

All responses use the existing `ApiResponse<T>` wrapper:

```json
{
  "result": "SUCCESS",
  "message": "...",
  "data": {}
}
```

### 4.1 Get Daily TimeSlots

| Item | Value |
| --- | --- |
| Method | `GET` |
| Path | `/api/time-slots` |
| Query Parameters | `userId`, `date` |
| Auth | Temporary explicit `userId`; future JWT user context |

#### Request Parameters

| Name | Type | Required | Validation |
| --- | --- | --- | --- |
| `userId` | Long | Yes | Positive |
| `date` | LocalDate | Yes | ISO date, e.g. `2026-05-16` |

#### Success Response

Status: `200 OK`

```json
{
  "result": "SUCCESS",
  "message": "Time slots retrieved successfully",
  "data": [
    {
      "id": 10,
      "date": "2026-05-16",
      "slotIndex": 36,
      "activityName": "写代码",
      "categoryId": 1,
      "categoryName": "工作",
      "categoryColor": "#3b82f6",
      "categoryStatus": "ACTIVE",
      "categoryDisplayName": "工作"
    }
  ]
}
```

#### Error Responses

| Scenario | Status | Message |
| --- | --- | --- |
| Missing or invalid date | `400 BAD_REQUEST` | Validation error message |
| Invalid userId | `400 BAD_REQUEST` | Validation error message |

### 4.2 Upsert TimeSlot

| Item | Value |
| --- | --- |
| Method | `PUT` |
| Path | `/api/time-slots` |
| Query Parameters | `userId` |
| Auth | Temporary explicit `userId`; future JWT user context |

#### Request Body

```json
{
  "date": "2026-05-16",
  "slotIndex": 36,
  "activityName": "写代码",
  "categoryId": 1
}
```

#### Validation Rules

| Field | Validation |
| --- | --- |
| `userId` | Required, positive |
| `date` | Required |
| `slotIndex` | Required, integer between 0 and 95 |
| `activityName` | Required, non-blank, max 100 characters |
| `categoryId` | Required, positive, must belong to user, must be ACTIVE |

#### Success Response

Status: `200 OK`

```json
{
  "result": "SUCCESS",
  "message": "Time slot saved successfully",
  "data": {
    "id": 10,
    "date": "2026-05-16",
    "slotIndex": 36,
    "activityName": "写代码",
    "categoryId": 1,
    "categoryName": "工作",
    "categoryColor": "#3b82f6",
    "categoryStatus": "ACTIVE",
    "categoryDisplayName": "工作"
  }
}
```

#### Error Responses

| Scenario | Status | Message |
| --- | --- | --- |
| `slotIndex` outside 0-95 | `400 BAD_REQUEST` | `slotIndex must be between 0 and 95` |
| Blank `activityName` | `400 BAD_REQUEST` | Validation error message |
| Category not found for user | `400 BAD_REQUEST` | `Category not found` |
| Category is archived | `400 BAD_REQUEST` | `Archived category cannot be used for new time slots` |

### 4.3 Delete TimeSlot

| Item | Value |
| --- | --- |
| Method | `DELETE` |
| Path | `/api/time-slots/{slotId}` |
| Query Parameters | `userId` |
| Auth | Temporary explicit `userId`; future JWT user context |

#### Path and Query Parameters

| Name | Type | Required | Validation |
| --- | --- | --- | --- |
| `slotId` | Long | Yes | Positive |
| `userId` | Long | Yes | Positive |

#### Success Response

Status: `200 OK`

```json
{
  "result": "SUCCESS",
  "message": "Time slot deleted successfully",
  "data": {
    "deleted": true,
    "slotId": 10
  }
}
```

#### Error Responses

| Scenario | Status | Message |
| --- | --- | --- |
| Slot does not exist or belongs to another user | `400 BAD_REQUEST` | `Time slot not found` |
| Invalid ID | `400 BAD_REQUEST` | Validation error message |

## 5. Business Logic

### 5.1 Slot Index Validation

- `slotIndex` must be between `0` and `95` inclusive.
- Start time can be derived by frontend or later backend helper logic:
  - total minutes = `slotIndex * 15`
  - hour = `totalMinutes / 60`
  - minute = `totalMinutes % 60`

### 5.2 Upsert / Overwrite Rule

When saving a TimeSlot:

1. Look up existing record by `userId + date + slotIndex`.
2. If found, overwrite `activityName`, `category`, and `updatedAt`.
3. If not found, create a new entity with `createdAt` and `updatedAt`.
4. Persist through `TimeSlotRepository.save`.

This avoids overlap logic and keeps the product aligned with the fixed-grid model.

### 5.3 User Data Isolation

Every repository lookup for user-owned TimeSlot records must include `userId`.

Required repository methods:

- `List<TimeSlot> findByUserIdAndDateOrderBySlotIndexAsc(Long userId, LocalDate date)`
- `Optional<TimeSlot> findByUserIdAndDateAndSlotIndex(Long userId, LocalDate date, Integer slotIndex)`
- `Optional<TimeSlot> findByIdAndUserId(Long id, Long userId)`

### 5.4 Category Validation

Upsert must load category with a user-scoped query:

- `CategoryRepository.findByUserIdAndId(userId, categoryId)`

Then validate:

- category exists;
- category status is `ACTIVE`.

### 5.5 Archived Category Display

Querying old TimeSlots must preserve category information even if the category is archived.

DTO display rules:

| Category State | `categoryDisplayName` |
| --- | --- |
| `ACTIVE` | category name |
| `ARCHIVED` | category name + ` (已归档)` |
| Missing category | `未知分类` |

## 6. Error Handling

The controller should follow existing backend conventions:

- Return `ResponseEntity<ApiResponse<?>>`.
- Keep controller methods wrapped in `try/catch` blocks.
- Delegate caught errors to `GlobalExceptionHandler.errorResponseEntity(...)`.

Expected errors:

| Error | Handling |
| --- | --- |
| Bean validation failure | `MethodArgumentNotValidException` handled globally as `400` |
| Invalid request parameter | `400` |
| TimeSlot not found | `IllegalArgumentException` -> `400` |
| Category not found | `IllegalArgumentException` -> `400` |
| Archived category selected | `IllegalArgumentException` -> `400` |
| Unexpected exception | global handler or controller catch -> structured error response |

## 7. Testing Strategy

Implementation must start with tests after this SDD is approved.

### 7.1 Unit Tests: TimeSlotServiceTest

Use Mockito for repository dependencies.

Test cases:

- `getDailySlots` returns user records ordered by `slotIndex`.
- `upsertTimeSlot` creates a new record when none exists.
- `upsertTimeSlot` overwrites existing record for same `userId + date + slotIndex`.
- `upsertTimeSlot` rejects `slotIndex < 0`.
- `upsertTimeSlot` rejects `slotIndex > 95`.
- `upsertTimeSlot` rejects category belonging to another user.
- `upsertTimeSlot` rejects archived category.
- `deleteTimeSlot` deletes only when slot belongs to current user.
- `deleteTimeSlot` throws when slot does not exist.

### 7.2 Integration Tests: TimeSlotIntegrationTest

Use `@SpringBootTest`, H2, and transactional tests.

Test cases:

- Create and query a slot for one user.
- Upsert the same date and slot index overwrites existing data instead of creating duplicates.
- Two users can use the same date and slot index independently.
- Query includes archived category display fields for historical records.
- Delete removes only the owner user's record.

### 7.3 Controller Tests: TimeSlotControllerTest

Use Spring test support where appropriate.

Test cases:

- `GET /api/time-slots` returns `ApiResponse<List<TimeSlotDto>>`.
- `PUT /api/time-slots` validates body and returns saved slot.
- `DELETE /api/time-slots/{slotId}` returns deletion result.
- Invalid request body returns structured error response.

## 8. Implementation Order After Approval

1. Add/adjust API contract documentation if needed.
2. Write JUnit 5 tests for service and integration scenarios.
3. Extend `TimeSlotRepository` with user-scoped query methods.
4. Add DTO records with compact canonical constructors where applicable.
5. Add `TimeSlotService` and `TimeSlotServiceImpl`.
6. Add `TimeSlotController`.
7. Run backend tests and fix failures.
8. Update `PROJECT_LOG.md`.

## 9. Current Blockers

- The repository currently has no Gradle Wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`).
- The current environment does not provide `gradle` in PATH.
- Because of this, JUnit validation cannot be executed locally until Gradle execution is restored.

Recommended resolution:

- Add Gradle Wrapper using a local/installed Gradle environment, or install Gradle on the machine and then run `gradle wrapper`.
- After wrapper files exist, validate with `interval-server/gradlew.bat test` on Windows.
