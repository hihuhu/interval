# 统计功能 SDD：分类耗时统计

## 1. Overview

统计功能用于帮助用户回顾在指定时间范围内，各个分类累计花费了多少时间。Interval 的最小记录单位是 15 分钟 `TimeSlot`，因此统计结果以已登记时间格数量为基础计算：

```text
分类耗时分钟数 = 该分类下 TimeSlot 数量 × 15
```

本阶段目标：

- 支持按日期范围统计分类耗时。
- 默认提供“今日 / 本周 / 本月 / 自定义日期范围”视图。
- 展示每个分类的总分钟数、小时文本、占比、颜色和记录格数。
- 展示总已记录时间与未记录时间概览。
- 前端新增统计页面，后端新增只读统计 API。

不在本阶段范围：

- 活动名称维度统计。
- 跨用户团队统计。
- 导出 CSV / 图片。
- 趋势折线图、日历热力图等高级报表。
- 对历史 TimeSlot 做快照汇总表；当前直接基于 `time_slots` 查询实时统计。

## 2. Architecture

### 2.1 Backend Layers

```text
StatsController
  -> StatsService interface
    -> StatsServiceImpl
      -> TimeSlotRepository
      -> CategoryRepository only if fallback category lookup is needed
        -> H2 Database
```

后端统计模块建议新增包：

```text
com.interval.stats
├── controller
│   └── StatsController.java
├── dto
│   ├── CategoryDurationStatDto.java
│   └── CategoryDurationSummaryDto.java
└── service
    ├── StatsService.java
    └── StatsServiceImpl.java
```

`TimeSlotRepository` 增加统计专用 JPQL 查询，使用 DTO 投影返回分类聚合结果，避免 ServiceImpl 直接查询数据库。

### 2.2 Frontend Layers

```text
StatsView
├── AppLayout
├── range toolbar
│   ├── quick range tabs: today / week / month
│   └── custom start/end date inputs
├── summary cards
│   ├── total recorded duration
│   ├── recorded slot count
│   └── unrecorded duration in selected range
└── category duration list/chart
    └── category stat row × N
```

前端建议新增：

```text
interval-client/src/views/StatsView.vue
interval-client/src/services/statsService.ts
interval-client/src/stores/useStatsStore.ts
interval-client/src/types/stats.ts
```

路由新增：

```text
/stats
```

路由名称：`stats`，需要登录访问。

## 3. Data Model

本阶段不新增数据库表，复用现有模型：

- `TimeSlot.userId`
- `TimeSlot.date`
- `TimeSlot.slotIndex`
- `TimeSlot.category`
- `Category.name`
- `Category.colorCode`
- `Category.status`

统计查询必须遵守用户隔离：只统计当前 JWT 用户的 TimeSlot。

### 3.1 Aggregation Rules

| 规则 | 说明 |
| --- | --- |
| 时间范围 | 闭区间：`startDate <= date <= endDate` |
| 单格时长 | 固定 15 分钟 |
| 总已记录分钟 | 所有匹配 TimeSlot 数量 × 15 |
| 分类分钟 | 该分类匹配 TimeSlot 数量 × 15 |
| 占比 | `categoryMinutes / totalRecordedMinutes`，总记录为 0 时返回 0 |
| 未记录分钟 | 日期范围天数 × 96 × 15 - totalRecordedMinutes |
| 分类排序 | 默认按 `durationMinutes` 降序；相同时按 `displayOrder`、名称排序 |
| 已归档分类 | 仍参与统计，并返回 `categoryStatus = ARCHIVED` |

### 3.2 DTO Shape

后端响应 DTO：

```java
public record CategoryDurationStatDto(
    Long categoryId,
    String categoryName,
    String categoryColor,
    String categoryStatus,
    Long slotCount,
    Long durationMinutes,
    Double percentage
) {}

public record CategoryDurationSummaryDto(
    LocalDate startDate,
    LocalDate endDate,
    Long totalSlotCount,
    Long totalRecordedMinutes,
    Long totalAvailableMinutes,
    Long unrecordedMinutes,
    List<CategoryDurationStatDto> categories
) {}
```

前端类型：

```typescript
interface CategoryDurationStat {
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  categoryStatus: 'ACTIVE' | 'ARCHIVED';
  slotCount: number;
  durationMinutes: number;
  percentage: number;
}

interface CategoryDurationSummary {
  startDate: string;
  endDate: string;
  totalSlotCount: number;
  totalRecordedMinutes: number;
  totalAvailableMinutes: number;
  unrecordedMinutes: number;
  categories: CategoryDurationStat[];
}
```

## 4. API Contract

所有响应继续使用标准 `ApiResponse<T>`。前端通过既有 Axios 拦截器自动附加 `Authorization: Bearer <token>`。

### 4.1 Get Category Duration Stats

| Item | Value |
| --- | --- |
| Method | `GET` |
| Path | `/api/stats/category-durations` |
| Auth | Required JWT |
| Query Parameters | `startDate`, `endDate` |

#### Request Parameters

| Name | Type | Required | Validation |
| --- | --- | --- | --- |
| `startDate` | LocalDate | Yes | ISO date, e.g. `2026-05-01` |
| `endDate` | LocalDate | Yes | ISO date, e.g. `2026-05-31` |

Validation rules:

- `startDate` and `endDate` must be present.
- `startDate` must be less than or equal to `endDate`.
- Date range should not exceed 366 days in this iteration, to prevent accidental heavy queries.

#### Success Response

Status: `200 OK`

```json
{
  "result": "SUCCESS",
  "message": "Category duration statistics retrieved successfully",
  "data": {
    "startDate": "2026-05-01",
    "endDate": "2026-05-31",
    "totalSlotCount": 120,
    "totalRecordedMinutes": 1800,
    "totalAvailableMinutes": 44640,
    "unrecordedMinutes": 42840,
    "categories": [
      {
        "categoryId": 1,
        "categoryName": "工作",
        "categoryColor": "#3b82f6",
        "categoryStatus": "ACTIVE",
        "slotCount": 64,
        "durationMinutes": 960,
        "percentage": 53.33
      },
      {
        "categoryId": 2,
        "categoryName": "学习",
        "categoryColor": "#22c55e",
        "categoryStatus": "ACTIVE",
        "slotCount": 40,
        "durationMinutes": 600,
        "percentage": 33.33
      }
    ]
  }
}
```

#### Error Responses

| Scenario | Status | Message |
| --- | --- | --- |
| Missing date parameter | `400 BAD_REQUEST` | Validation error message |
| Invalid date format | `400 BAD_REQUEST` | Validation error message |
| `startDate > endDate` | `400 BAD_REQUEST` | `startDate must be before or equal to endDate` |
| Date range too large | `400 BAD_REQUEST` | `Date range must not exceed 366 days` |
| Missing or invalid JWT | `401 UNAUTHORIZED` | Authentication error message |
| Unexpected server error | `500 INTERNAL_SERVER_ERROR` | Generic error message |

## 5. Business Logic

### 5.1 Backend Service Logic

`StatsService.getCategoryDurations(startDate, endDate)`：

1. 从安全上下文获取当前用户 ID。
2. 校验日期范围。
3. 调用 `TimeSlotRepository` 聚合查询当前用户在范围内的 TimeSlot：
   - 按分类 ID、分类名称、颜色、状态、显示顺序分组。
   - 统计 `COUNT(ts.id)`。
4. 计算：
   - `durationMinutes = slotCount × 15`
   - `totalSlotCount = sum(slotCount)`
   - `totalRecordedMinutes = totalSlotCount × 15`
   - `totalAvailableMinutes = daysBetweenInclusive × 96 × 15`
   - `unrecordedMinutes = totalAvailableMinutes - totalRecordedMinutes`
   - `percentage = durationMinutes / totalRecordedMinutes × 100`
5. 按耗时降序输出分类列表。
6. 返回 `CategoryDurationSummaryDto`。

### 5.2 Repository Query

建议使用 JPQL DTO 投影，例如：

```text
SELECT new com.interval.stats.dto.CategoryDurationStatRawDto(
  c.id,
  c.name,
  c.colorCode,
  c.status,
  c.displayOrder,
  COUNT(ts.id)
)
FROM TimeSlot ts
JOIN ts.category c
WHERE ts.userId = :userId
  AND ts.date BETWEEN :startDate AND :endDate
GROUP BY c.id, c.name, c.colorCode, c.status, c.displayOrder
ORDER BY COUNT(ts.id) DESC, c.displayOrder ASC, c.name ASC
```

可新增内部 raw DTO 承接 `displayOrder` 与 `slotCount`，ServiceImpl 再转换为对外 DTO。

### 5.3 Frontend Logic

- `statsService.getCategoryDurations(startDate, endDate)` 调用 API 并返回解包后的 `CategoryDurationSummary`。
- `useStatsStore` 保存：
  - `summary`
  - `loading`
  - `errorMessage`
  - `selectedRange`
- `StatsView` 提供快捷范围：
  - 今日：当天到当天。
  - 本周：周一到周日。
  - 本月：当月 1 日到当月最后一天。
  - 自定义：用户手动选择开始和结束日期。
- UI 展示：
  - 顶部汇总卡片：总记录时长、总格数、未记录时长。
  - 分类列表：颜色点、分类名称、归档标记、时长、占比进度条。
  - 空状态：范围内没有记录时提示“这个时间范围还没有记录”。

## 6. UI/UX Considerations

### 6.1 Page Entry

在主布局导航中增加“统计”入口，与当前 Time Grid 页面并列。

### 6.2 Display Format

分钟数在 UI 中格式化为更易读文本：

| Minutes | Display |
| --- | --- |
| 15 | `15 分钟` |
| 60 | `1 小时` |
| 75 | `1 小时 15 分钟` |
| 600 | `10 小时` |

### 6.3 Category Rows

每个分类统计行展示：

```text
[颜色] 工作                 16 小时      53.33%
       ███████████████░░░░░░░░░░
       64 个时间块 · ACTIVE
```

已归档分类显示为：

```text
工作（已归档）
```

### 6.4 Loading and Error States

- 首次进入页面默认加载“今日”。
- 切换快捷范围或自定义日期后自动重新加载。
- 加载中显示骨架或轻量 loading 文案。
- API 错误显示页面级错误提示，并保留上一次成功结果，避免页面突然清空。

## 7. Error Handling

- Controller 方法必须返回 `ResponseEntity<ApiResponse<?>>`。
- Controller 逻辑放在 `try..catch` 中，并将异常交给 `GlobalExceptionHandler` 包装。
- 日期范围业务错误使用 `IllegalArgumentException`，返回 400。
- 未认证请求由现有 Spring Security 机制返回 401。
- 前端 401 继续由 Axios 响应拦截器清理登录态并跳转登录页。

## 8. Testing Strategy

### 8.1 Backend JUnit 5 Tests

建议新增：

- `StatsServiceImplTest`
  - happy path：多个分类在日期范围内正确聚合分钟数和百分比。
  - archived category：归档分类仍参与统计。
  - empty range：无记录时返回总记录 0、分类空数组、未记录分钟正确。
  - invalid range：`startDate > endDate` 抛出 `IllegalArgumentException`。
  - too large range：超过 366 天抛出 `IllegalArgumentException`。

- `StatsControllerTest` 或集成测试
  - GET `/api/stats/category-durations` 成功返回标准 `ApiResponse`。
  - 缺少日期参数返回 400。
  - 未登录返回 401。

### 8.2 Frontend Vitest Tests

建议新增：

- `statsService.test.ts`
  - 调用正确 endpoint 与 query params。
  - 返回解包后的 summary。

- `useStatsStore.test.ts`
  - 成功加载 summary。
  - 失败时设置 errorMessage。
  - 切换 range 后重新请求。

- `StatsView.test.ts`
  - 默认渲染今日统计。
  - 渲染分类耗时、百分比和进度条。
  - 空状态显示正确。
  - 自定义日期范围校验与触发加载。

## 9. Acceptance Criteria

- 用户可以从导航进入统计页面。
- 用户可以查看今日、本周、本月、自定义日期范围的分类耗时。
- 每个分类显示名称、颜色、耗时、时间块数量、占比。
- 已归档分类仍能在历史统计中展示，并标注“已归档”。
- 无记录范围显示空状态，不报错。
- 后端统计 API 只返回当前登录用户的数据。
- 后端 JUnit 5 测试通过。
- 前端 Vitest 测试通过。
- 前端生产构建通过。

## 10. Prototype First Scope

根据当前交互确认节奏，本统计功能先新增一个静态 HTML 原型用于评估信息架构、视觉层级和基础交互，不连接真实后端 API，也不进入 Vue 正式实现。

原型文件：

```text
docs/prototypes/stats-category-duration.html
```

当前状态：原型已完成，可作为第一版正式统计页面的视觉和交互参考。

原型目标：

- 延续 `modern-time-grid-v2.html` 的轻量白底、卡片、紫色强调视觉风格。
- 展示统计页面入口、范围选择、汇总卡片、分类占比列表和空状态。
- 使用内置模拟数据展示“今日 / 本周 / 本月 / 自定义”切换效果。
- 支持按分类点击高亮，用于观察右侧详情摘要。
- 支持展示已归档分类仍参与历史统计。
- 不包含真实 API、JWT、Vue Router 或正式组件拆分。

原型确认标准：

- 页面能清楚回答“每个分类花了多长时间”。
- 汇总区不会喧宾夺主，重点仍是分类耗时。
- 空状态、已归档分类和不同统计范围都有明确表现。
- 信息密度适合桌面端第一版；移动端只要求基础可读，不作为本阶段重点。

原型通过后，再进入测试和正式实现阶段。

## 11. Implementation Order After Approval

1. 确认统计原型效果与本 SDD。
2. 后端新增 JUnit 5 测试：Service 聚合、日期校验、空结果、归档分类、Controller 授权与响应结构。
3. 后端实现 Stats DTO、Service、Repository 查询、Controller。
4. 运行后端测试并修复问题。
5. 前端新增类型、service、store、路由与 `StatsView` 测试。
6. 前端实现统计页面与导航入口。
7. 运行前端测试和生产构建。
8. 使用真实 API 做本地冒烟验证：登录、新建分类、登记 TimeSlot、访问统计页并核对分类耗时。
