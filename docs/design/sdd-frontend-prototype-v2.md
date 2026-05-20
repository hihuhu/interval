# 前端原型 v2 设计文档 (SDD)

## 1. 概述

本文档描述 Interval 项目前端原型 v2 的设计改进，主要针对用户反馈的三个核心问题：
1. 日期选择体验不佳
2. 弹窗交互打断工作流
3. 缺少统计分析功能

**文件位置**: `docs/prototypes/modern-time-grid-v2.html`

---

## 2. 核心改进

### 2.1 日期选择器重新设计

#### 问题
- 前后按钮不够明显
- 无法选择具体某一天
- 只能逐日切换效率低

#### 解决方案
**日历弹窗组件**
- 点击日期按钮弹出完整日历面板
- 支持月份切换（上月/下月按钮）
- 日历网格显示当月所有日期
- 视觉标识：
  - 今天：浅蓝色背景 + 蓝色文字
  - 选中日期：蓝色背景 + 白色文字
  - 非当月日期：灰色文字

**导航按钮增强**
- 前后按钮尺寸增大（40x40px）
- 添加 2px 边框和 hover 效果
- SVG 箭头图标更清晰
- "今天"按钮带背景色突出显示

#### UI 规格
```
日历弹窗：
- 宽度：320px
- 阴影：shadow-2xl
- 圆角：16px
- 网格：7列（周日-周六）
- 单元格：36x36px，圆角 8px

导航按钮：
- 尺寸：40x40px
- 边框：2px solid #e5e7eb
- Hover：边框变蓝 + 背景浅蓝
```

---

### 2.2 侧边栏展示替代弹窗

#### 问题
- 弹窗打断工作流
- 需要额外点击关闭
- 无法同时查看网格和编辑信息

#### 解决方案
**右侧固定面板**
- 选中时间块后，右侧面板实时显示选择信息
- 无需弹窗，所有操作在侧边栏完成
- 面板宽度：320px

**显示内容**
1. **选择摘要**
   - 选中数量（N 个）
   - 总时长（N 分钟）
   - 时间范围（HH:MM — HH:MM）

2. **分类选择**
   - 滚动列表显示所有活跃分类
   - 点击选中，带视觉反馈（蓝色背景 + 边框）

3. **备注输入**
   - 多行文本框（3行）
   - 占位符提示："这段时间做了什么..."

4. **操作按钮**
   - 保存：主按钮（渐变蓝色）
   - 取消：次要按钮（灰色）

#### 交互流程
```
1. 用户选择时间块（拖拽/点击）
   ↓
2. 右侧面板自动显示选择信息
   ↓
3. 用户选择分类 + 填写备注
   ↓
4. 点击保存 → 清空选择 + 更新网格
```

---

### 2.3 时间块选择交互设计（完整场景）

#### 核心交互模式

**模式1：拖拽选择（连续块）**
- 按住鼠标拖拽，选中连续的时间块
- 开始新拖拽时，自动清空之前的选择

**模式2：单击选择（跳选不连续块）**
- 单击未选中的块：添加到选择集
- 单击已选中的块：从选择集中移除

**模式3：混合使用**
- 先拖拽选择一段，再单击添加其他块
- 先单击几个块，再拖拽会清空旧选择

#### 完整交互场景矩阵

**场景组1：纯单击操作**

场景1.1：用户上来就单击选块
```
初始状态：所有块都是原始状态（空白或已登记）

操作：单击 09:00
结果：09:00 变成深蓝色遮罩
状态：选中 1 个块

操作：单击 11:00
结果：11:00 变成深蓝色遮罩
状态：选中 2 个块（09:00 + 11:00）

操作：单击 14:00
结果：14:00 变成深蓝色遮罩
状态：选中 3 个块（09:00 + 11:00 + 14:00）
```

场景1.2：单击已选中的块（取消选择）
```
当前状态：已选中 09:00、11:00、14:00（3个块）

操作：单击 11:00（已选中的块）
结果：11:00 恢复原始颜色，遮罩消失
状态：选中 2 个块（09:00 + 14:00）
```

**场景组2：先单击，再拖选**

场景2.1：先单击几个块，然后拖选
```
操作1：单击 09:00
结果：09:00 变深蓝色遮罩
状态：选中 1 个块

操作2：单击 11:00
结果：11:00 变深蓝色遮罩
状态：选中 2 个块（09:00 + 11:00）

操作3：拖拽 14:00 → 15:00
结果：
  - 09:00 和 11:00 立即恢复原色（清空旧选择）
  - 14:00-15:00 变成深蓝色遮罩（新选择）
状态：选中 4 个块（14:00-15:00）
```

**场景组3：先拖选，再单击**

场景3.1：先拖选一段，然后单击添加
```
操作1：拖拽 09:00 → 09:45
结果：09:00-09:45（3个块）变深蓝色遮罩
状态：选中 3 个块

操作2：单击 11:00
结果：11:00 变深蓝色遮罩，添加到选择集
状态：选中 4 个块（09:00-09:45 + 11:00）

操作3：单击 14:00
结果：14:00 变深蓝色遮罩，添加到选择集
状态：选中 5 个块（09:00-09:45 + 11:00 + 14:00）
```

场景3.2：先拖选，单击添加，再单击取消
```
当前状态：已选中 09:00-09:45 + 11:00 + 14:00（5个块）

操作：单击 09:15（已选中的块）
结果：09:15 恢复原色，从选择集中移除
状态：选中 4 个块（09:00、09:30、09:45 + 11:00 + 14:00）
```

**场景组4：混合操作**

场景4.1：拖选 → 单击添加 → 再拖选（清空重来）
```
操作1：拖拽 09:00 → 09:45
状态：选中 3 个块

操作2：单击 11:00
状态：选中 4 个块（09:00-09:45 + 11:00）

操作3：拖拽 14:00 → 15:00
结果：
  - 之前的 4 个块全部恢复原色（清空）
  - 14:00-15:00 变深蓝色（新选择）
状态：选中 4 个块（14:00-15:00）
```

场景4.2：单击 → 拖选 → 单击
```
操作1：单击 09:00
状态：选中 1 个块

操作2：拖拽 11:00 → 11:30
结果：09:00 清空，11:00-11:30 选中
状态：选中 2 个块（11:00-11:30）

操作3：单击 14:00
结果：14:00 添加到选择集
状态：选中 3 个块（11:00-11:30 + 14:00）
```

#### 视觉反馈设计：遮罩效果

**问题**：选中后完全覆盖原始颜色，无法区分空白块和已登记块

**解决方案**：使用半透明深蓝色遮罩层

**CSS 实现**：
```css
/* 选中状态（遮罩效果） */
.grid-cell.selected {
  position: relative;
  box-shadow: 0 0 0 4px #4338ca inset;
  transform: scale(1.05);
}

/* 深蓝色遮罩层 */
.grid-cell.selected::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(99, 102, 241, 0.75);  /* 75% 不透明度 */
  pointer-events: none;
  z-index: 1;
}

/* 确保内容在遮罩之上 */
.grid-cell.selected > * {
  position: relative;
  z-index: 2;
}
```

**视觉效果**：
```
选中前：
┌─────┬─────┬─────┐
│工作 │空白 │运动 │
│浅蓝 │白色 │绿色 │
└─────┴─────┴─────┘

选中后（遮罩效果）：
┌─────────┬─────────┬─────────┐
│  工作   │  空白   │  运动   │  ← 底层颜色
│ ▓▓▓▓▓ │ ▓▓▓▓▓ │ ▓▓▓▓▓ │  ← 深蓝遮罩（75%透明度）
│浅蓝+蓝 │白色+蓝 │绿色+蓝 │  ← 混合效果
└─────────┴─────────┴─────────┘

说明：
- 空白块：白色 + 深蓝遮罩 = 纯深蓝色
- 工作块：浅蓝色 + 深蓝遮罩 = 深蓝色（能隐约看到底层的浅蓝）
- 运动块：绿色 + 深蓝遮罩 = 蓝绿混合色（能隐约看到底层的绿色）
```

#### 交互逻辑总结表

| 当前状态 | 操作 | 结果 | 说明 |
|------|------|------|------|
| 无选择 | 单击块A | 块A变深蓝遮罩 | 开始选择 |
| 已选A | 单击块B（未选中） | 块B变深蓝遮罩 | 添加到选择集 |
| 已选A、B | 单击块A（已选中） | 块A恢复原色 | 取消选择 |
| 已选A、B | 拖拽C→D | A、B恢复原色，C→D变深蓝 | 清空重选 |
| 无选择 | 拖拽A→C | A→C变深蓝遮罩 | 连续选择 |
| 已选A→C | 单击块D | 块D变深蓝遮罩 | 添加不连续块 |
| 已选A→C、D | 拖拽E→F | 全部清空，E→F变蓝色遮罩 | 重新拖选 |
| 选中内容包含已登记块 | 点击右侧“擦除已登记数据” | 删除选中块中已有数据的记录，未登记块不受影响 | 用于清理误填数据 |

#### 右侧擦除功能

**触发条件**：当前选择集中至少包含 1 个已登记时间块。

**显示位置**：右侧“已选择时间块”面板，在覆盖提示下方显示危险操作按钮。

**按钮文案**：`擦除已登记数据`。

**行为规则**：
- 只擦除当前选中块中已有登记数据的块。
- 未登记块保持未登记状态，不做额外处理。
- 擦除内容包括分类记录与备注记录。
- 擦除后清空当前选择，避免用户误以为仍处于编辑状态。
- 原型中直接本地删除；正式实现时对应调用 TimeSlot 删除 API 或批量删除 API。

**交互场景**：
```
当前状态：用户选中 09:00、09:15、11:00，其中 09:00 和 11:00 已登记。
右侧提示：检测到已有记录，选中的 2 个块已登记。
操作：点击“擦除已登记数据”。
结果：
  - 09:00 分类与备注被删除，恢复空白块。
  - 11:00 分类与备注被删除，恢复空白块。
  - 09:15 原本未登记，不变化。
  - 当前选择清空。
```

---

### 2.4 多选功能增强（旧版本）

#### 问题
- 只支持连续拖拽选择
- 无法选择不连续的时间块
- 已登记的块无法重新选择和编辑

#### 解决方案
**统一拖拽选择**

**拖拽选择（适用于所有块）**
- 鼠标按下 → 拖动 → 松开
- 选中起点到终点之间的所有格子
- 实时高亮拖拽范围（浅蓝色虚框）
- **已登记的块也可以被拖拽选择**

**点击已登记块回显**
- 单击已登记的时间块
- 自动打开登记弹窗
- 回显该块的分类和备注信息
- 用户可以修改后重新保存（覆盖原有内容）

**视觉反馈**
- 已选格子：蓝色边框 + 浅蓝色背景
- 拖拽中格子：更深的蓝色边框
- 已填充格子：保持原分类颜色 + 选中边框

#### 实现细节

**拖拽逻辑（简化版）**

```javascript
function startDrag(idx) {
  // 所有块统一处理：开始拖拽选择
  dragging.value = true;
  dragStart.value = idx;
  dragEnd.value = idx;
  dragSel.value = [];
}

function moveDrag(idx) {
  if (dragging.value) dragEnd.value = idx;
}

function endDrag() {
  if (!dragging.value) return;
  dragging.value = false;
  const mn = Math.min(dragStart.value, dragEnd.value);
  const mx = Math.max(dragStart.value, dragEnd.value);
  const sel = [];
  for (let i = mn; i <= mx; i++) sel.push(i);
  dragSel.value = sel;
  dragStart.value = -1;
  dragEnd.value = -1;
}
```

**点击回显逻辑**

```javascript
function cellClick(idx, e) {
  // 如果正在拖拽，不处理点击
  if (dragging.value) return;
  
  // 检查是否已登记
  const existing = getCat(idx);
  if (existing) {
    e.stopPropagation();
    
    // 回显分类
    modalIdx.value = idx;
    modalCat.value = existing;
    
    // 回显备注
    const key = dk.value + '_' + idx;
    modalNote.value = notes.value[key] || '';
    
    // 设置选择状态
    dragSel.value = [idx];
    
    // 打开弹窗
    modal.value = true;
  }
}
```

**覆盖提示逻辑**

```javascript
// 检测是否有已登记的块
const hasOverlap = computed(() => {
  if (dragSel.value.length === 0) return false;
  const daySlots = slots.value[dk.value] || {};
  return dragSel.value.some(idx => daySlots[idx] !== undefined);
});

// 统计已登记块的数量
const overlapCount = computed(() => {
  if (dragSel.value.length === 0) return 0;
  const daySlots = slots.value[dk.value] || {};
  return dragSel.value.filter(idx => daySlots[idx] !== undefined).length;
});
```

**保存逻辑（支持覆盖）**

```javascript
function saveSlot() {
  if (!modalCat.value) return;
  if (!slots.value[dk.value]) slots.value[dk.value] = {};
  
  const targets = dragSel.value.length > 0 ? dragSel.value : [modalIdx.value];
  
  // 批量保存（覆盖已有记录）
  targets.forEach(idx => {
    slots.value[dk.value][idx] = modalCat.value.id;
    if (modalNote.value.trim()) {
      notes.value[dk.value + '_' + idx] = modalNote.value.trim();
    }
  });
  
  modal.value = false;
  dragSel.value = [];
}
```

---

### 2.4 统计分析功能

#### 需求
- 查看时间使用情况
- 分析分类分布
- 追踪时间趋势

#### 解决方案
**双视图切换**
- 顶部导航栏添加标签页切换
- 视图 1：时间网格（原有功能）
- 视图 2：统计分析（新增）

**统计视图布局**

**顶部卡片（3列）**
1. **今日总时长**
   - 显示：Xh Ym
   - 副标题：共 N 个时间块
   - 图标：时钟

2. **本周总时长**
   - 显示：Xh
   - 副标题：↑ 比上周多 X.Xh
   - 图标：趋势线

3. **最常用分类**
   - 显示：分类名称
   - 副标题：占比 X%
   - 图标：星标

**底部图表（2列）**

**左侧：分类分布**
- 列表显示所有有记录的分类
- 每个分类显示：
  - 分类名称 + 颜色圆点
  - 时长（Xh Ym）
  - 占比百分比
  - 进度条（宽度 = 占比，颜色 = 分类颜色）
- 按占比降序排列

**右侧：最近7天趋势**
- 横向条形图
- X轴：日期（月/日）
- Y轴：时长（小时）
- 条形颜色：渐变蓝色
- 条形内显示具体小时数

#### 数据计算逻辑
```javascript
// 本周总时长
const weekHours = computed(() => {
  let total = 0;
  for (let i = 0; i < 7; i++) {
    const d = new Date(date.value);
    d.setDate(d.getDate() - i);
    const key = formatDateKey(d);
    total += Object.keys(slots.value[key] || {}).length * 15;
  }
  return Math.floor(total / 60);
});

// 分类分布
const statsCategories = computed(() => {
  const daySlots = slots.value[dk.value] || {};
  const catCounts = {};
  Object.values(daySlots).forEach(catId => {
    catCounts[catId] = (catCounts[catId] || 0) + 1;
  });
  const total = Object.values(catCounts).reduce((a, b) => a + b, 0);
  return cats.value.map(cat => {
    const count = catCounts[cat.id] || 0;
    const mins = count * 15;
    return {
      ...cat,
      hours: Math.floor(mins / 60),
      mins: mins % 60,
      percent: total > 0 ? Math.round(count / total * 100) : 0
    };
  }).filter(c => c.percent > 0).sort((a, b) => b.percent - a.percent);
});
```

---

## 3. 后端逻辑改动评估

### 3.1 现有后端 API 回顾

**TimeSlot API**
```
PUT /api/time-slots
- 请求体：{ date, slotIndex, categoryId, note }
- 功能：单格 upsert

GET /api/time-slots?date=YYYY-MM-DD
- 返回：当日所有 TimeSlot 列表

DELETE /api/time-slots/{id}
- 功能：删除单个 TimeSlot
```

### 3.2 需要的后端改动

#### 改动 1：批量保存 TimeSlot（推荐）

**问题**
- 前端多选后需要循环调用 PUT API
- 网络开销大，用户体验差
- 例如：选择 10 个格子 = 10 次 HTTP 请求

**解决方案**
新增批量保存 API

```
POST /api/time-slots/batch
请求体：
{
  "date": "2024-05-18",
  "slots": [
    { "slotIndex": 36, "categoryId": 1, "note": "处理邮件" },
    { "slotIndex": 37, "categoryId": 1, "note": "处理邮件" },
    { "slotIndex": 38, "categoryId": 1, "note": "处理邮件" }
  ]
}

响应：
{
  "result": "SUCCESS",
  "message": "批量保存成功",
  "data": {
    "savedCount": 3,
    "slots": [ /* 保存后的 TimeSlot 列表 */ ]
  }
}
```

**后端实现要点**
```java
@PostMapping("/batch")
public ApiResponse<BatchSaveResponseDto> batchSaveTimeSlots(
    @AuthenticationPrincipal AuthenticatedUser user,
    @RequestBody @Valid BatchSaveRequest request
) {
    List<TimeSlotDto> saved = timeSlotService.batchUpsert(
        user.getUserId(),
        request.getDate(),
        request.getSlots()
    );
    return ApiResponse.success(
        "批量保存成功",
        new BatchSaveResponseDto(saved.size(), saved)
    );
}
```

**Service 层逻辑**
```java
@Transactional
public List<TimeSlotDto> batchUpsert(
    Long userId,
    LocalDate date,
    List<SlotData> slots
) {
    List<TimeSlot> entities = new ArrayList<>();
    
    for (SlotData slot : slots) {
        // 查找已存在的记录
        Optional<TimeSlot> existing = timeSlotRepository
            .findByUserIdAndDateAndSlotIndex(userId, date, slot.getSlotIndex());
        
        TimeSlot entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setCategoryId(slot.getCategoryId());
            entity.setNote(slot.getNote());
        } else {
            entity = new TimeSlot();
            entity.setUserId(userId);
            entity.setDate(date);
            entity.setSlotIndex(slot.getSlotIndex());
            entity.setCategoryId(slot.getCategoryId());
            entity.setNote(slot.getNote());
        }
        entities.add(entity);
    }
    
    List<TimeSlot> saved = timeSlotRepository.saveAll(entities);
    return saved.stream().map(this::toDto).collect(Collectors.toList());
}
```

#### 改动 2：统计 API（推荐）

**问题**
- 前端需要自己计算统计数据
- 跨日期查询需要多次请求
- 计算逻辑复杂且重复

**解决方案**
新增统计查询 API

```
GET /api/time-slots/stats?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
响应：
{
  "result": "SUCCESS",
  "data": {
    "totalMinutes": 450,
    "totalSlots": 30,
    "categoryStats": [
      {
        "categoryId": 1,
        "categoryName": "工作",
        "categoryColor": "#a5b4fc",
        "minutes": 180,
        "slots": 12,
        "percentage": 40
      }
    ],
    "dailyStats": [
      {
        "date": "2024-05-18",
        "minutes": 450,
        "slots": 30
      }
    ]
  }
}
```

**后端实现要点**
```java
@GetMapping("/stats")
public ApiResponse<TimeSlotStatsDto> getStats(
    @AuthenticationPrincipal AuthenticatedUser user,
    @RequestParam LocalDate startDate,
    @RequestParam LocalDate endDate
) {
    TimeSlotStatsDto stats = timeSlotService.calculateStats(
        user.getUserId(),
        startDate,
        endDate
    );
    return ApiResponse.success("统计查询成功", stats);
}
```

**Service 层逻辑**
```java
public TimeSlotStatsDto calculateStats(
    Long userId,
    LocalDate startDate,
    LocalDate endDate
) {
    List<TimeSlot> slots = timeSlotRepository
        .findByUserIdAndDateBetween(userId, startDate, endDate);
    
    int totalSlots = slots.size();
    int totalMinutes = totalSlots * 15;
    
    // 按分类统计
    Map<Long, List<TimeSlot>> byCategory = slots.stream()
        .collect(Collectors.groupingBy(TimeSlot::getCategoryId));
    
    List<CategoryStatDto> categoryStats = byCategory.entrySet().stream()
        .map(entry -> {
            Long catId = entry.getKey();
            List<TimeSlot> catSlots = entry.getValue();
            Category category = categoryRepository.findById(catId).orElse(null);
            
            int count = catSlots.size();
            int minutes = count * 15;
            int percentage = (int) Math.round(count * 100.0 / totalSlots);
            
            return new CategoryStatDto(
                catId,
                category != null ? category.getName() : "未知",
                category != null ? category.getColorCode() : "#cccccc",
                minutes,
                count,
                percentage
            );
        })
        .sorted(Comparator.comparing(CategoryStatDto::getPercentage).reversed())
        .collect(Collectors.toList());
    
    // 按日期统计
    Map<LocalDate, List<TimeSlot>> byDate = slots.stream()
        .collect(Collectors.groupingBy(TimeSlot::getDate));
    
    List<DailyStatDto> dailyStats = byDate.entrySet().stream()
        .map(entry -> {
            LocalDate date = entry.getKey();
            int count = entry.getValue().size();
            return new DailyStatDto(date, count * 15, count);
        })
        .sorted(Comparator.comparing(DailyStatDto::getDate))
        .collect(Collectors.toList());
    
    return new TimeSlotStatsDto(
        totalMinutes,
        totalSlots,
        categoryStats,
        dailyStats
    );
}
```

**Repository 新增方法**
```java
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    // 现有方法...
    
    // 新增：日期范围查询
    List<TimeSlot> findByUserIdAndDateBetween(
        Long userId,
        LocalDate startDate,
        LocalDate endDate
    );
    
    // 新增：批量查询优化
    @Query("SELECT t FROM TimeSlot t " +
           "LEFT JOIN FETCH t.category " +
           "WHERE t.userId = :userId " +
           "AND t.date BETWEEN :startDate AND :endDate")
    List<TimeSlot> findByUserIdAndDateBetweenWithCategory(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
```

#### 改动 3：备注字段（必需）

**问题**
- 当前 TimeSlot Entity 可能没有 note 字段

**解决方案**
确保 TimeSlot Entity 包含 note 字段

```java
@Entity
@Table(name = "time_slots")
public class TimeSlot {
    // 现有字段...
    
    @Column(name = "note", length = 500)
    private String note;  // 新增或确认存在
    
    // getter/setter
}
```

**数据库迁移**
```sql
-- 如果字段不存在，添加字段
ALTER TABLE time_slots 
ADD COLUMN note VARCHAR(500);
```

---

## 4. 前后端交互流程

### 4.1 批量保存流程

```
前端：
1. 用户拖拽选择 10 个时间块
2. 选择分类 "工作"
3. 填写备注 "处理邮件和晨会"
4. 点击保存

前端发送：
POST /api/time-slots/batch
{
  "date": "2024-05-18",
  "slots": [
    { "slotIndex": 36, "categoryId": 1, "note": "处理邮件和晨会" },
    { "slotIndex": 37, "categoryId": 1, "note": "处理邮件和晨会" },
    ...
  ]
}

后端处理：
1. 验证用户权限
2. 验证分类归属
3. 批量 upsert（事务）
4. 返回保存结果

前端接收：
{
  "result": "SUCCESS",
  "data": {
    "savedCount": 10,
    "slots": [ /* 完整的 TimeSlot 列表 */ ]
  }
}

前端更新：
1. 清空选择状态
2. 更新网格显示
3. 显示成功提示
```

### 4.2 统计查询流程

```
前端：
1. 用户切换到"统计分析"标签
2. 前端计算日期范围（今天 & 最近7天）

前端发送：
GET /api/time-slots/stats?startDate=2024-05-12&endDate=2024-05-18

后端处理：
1. 查询日期范围内的所有 TimeSlot
2. 按分类聚合统计
3. 按日期聚合统计
4. 计算百分比和排序

前端接收：
{
  "result": "SUCCESS",
  "data": {
    "totalMinutes": 2700,
    "totalSlots": 180,
    "categoryStats": [ /* 分类统计 */ ],
    "dailyStats": [ /* 每日统计 */ ]
  }
}

前端渲染：
1. 顶部卡片显示总时长
2. 分类分布图显示占比
3. 趋势图显示每日数据
```

---

## 5. 实现优先级

### P0（必需）
1. ✅ 前端原型 v2（已完成）
2. ⏳ TimeSlot Entity 添加 note 字段
3. ⏳ 批量保存 API（POST /api/time-slots/batch）

### P1（推荐）
4. ⏳ 统计查询 API（GET /api/time-slots/stats）
5. ⏳ Repository 添加日期范围查询方法

### P2（优化）
6. ⏳ 前端集成真实后端 API
7. ⏳ 性能优化（批量查询、索引）
8. ⏳ 单元测试和集成测试

---

## 6. 数据库索引建议

为支持统计查询性能，建议添加以下索引：

```sql
-- 用户 + 日期范围查询索引
CREATE INDEX idx_time_slots_user_date 
ON time_slots(user_id, date);

-- 用户 + 日期 + 分类查询索引
CREATE INDEX idx_time_slots_user_date_category 
ON time_slots(user_id, date, category_id);
```

---

## 7. 前端技术栈

- **框架**: Vue 3 Composition API
- **样式**: Tailwind CSS (CDN)
- **字体**: Inter (Google Fonts)
- **图标**: 内联 SVG
- **状态管理**: Vue ref/computed
- **文件大小**: ~25KB (单文件)

---

## 8. 浏览器兼容性

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- 不支持 IE11

---

## 9. 后续优化方向

1. **拖拽体验优化**
   - 添加拖拽预览
   - 支持触摸设备

2. **统计功能扩展**
   - 月度/年度统计
   - 导出报表（CSV/PDF）
   - 目标设置和进度追踪

3. **性能优化**
   - 虚拟滚动（大量数据）
   - 懒加载历史数据
   - 离线缓存

4. **协作功能**
   - 团队统计
   - 时间对比
   - 分享报告

---

## 10. 验收标准

### 前端原型
- ✅ 日历弹窗可选择任意日期
- ✅ 前后按钮清晰可见
- ✅ 侧边栏实时显示选择信息
- ✅ 支持拖拽连续选择
- ✅ 支持 Ctrl+点击不连续选择
- ✅ 统计视图显示完整数据
- ✅ 所有中文正确显示

### 后端 API
- ⏳ 批量保存 API 返回正确结果
- ⏳ 统计 API 计算准确
- ⏳ 响应时间 < 500ms
- ⏳ 支持并发请求
- ⏳ 错误处理完善

---

**文档版本**: v1.0  
**创建日期**: 2024-05-18  
**最后更新**: 2024-05-18  
**作者**: Interval Team

---

## 11. 备注显示与回显功能

### 11.1 功能概述

为了提升用户体验，v2 版本新增了备注的可视化展示和编辑回显功能：
- 鼠标悬停时显示备注内容
- 点击已登记块时自动回显分类和备注
- 支持批量备注共享

### 11.2 Hover 悬浮提示

**触发条件**
- 鼠标悬停在已登记且有备注的时间块上
- 使用 CSS group-hover 实现

**显示内容**
- 分类名称（加粗，白色文字）
- 备注内容（10px 字体，灰色文字）
- 深色背景（#1e293b）
- 带有指向时间块的小三角箭头

**样式规格**
```css
.tooltip {
  position: absolute;
  bottom: 100%;
  width: 192px;
  padding: 8px 12px;
  background: #1e293b;
  color: white;
  font-size: 11px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  z-index: 10;
}
```

### 11.3 点击回显逻辑

**实现函数**

```javascript
function cellClick(idx, e) {
  if (dragging.value) return;
  const existing = getCat(idx);
  if (existing) {
    e.stopPropagation();
    modalIdx.value = idx;
    modalCat.value = existing;
    const key = dk.value + '_' + idx;
    modalNote.value = notes.value[key] || '';
    dragSel.value = [idx];
    modal.value = true;
  }
}

function cellNote(idx) {
  const key = dk.value + '_' + idx;
  return notes.value[key] || '';
}

function cellCatName(idx) {
  const c = getCat(idx);
  return c ? c.name : '';
}
```

**交互流程**
1. 用户单击已登记的时间块
2. 检测该块是否有分类信息
3. 如果有，打开登记弹窗
4. 自动回显分类（高亮选中）
5. 自动回显备注内容（填充到文本框）
6. 用户可以修改后保存（覆盖原有内容）

### 11.4 批量备注共享

**存储机制**
- 每个时间块独立存储备注
- 使用 notes 对象，键格式：{date}_{slotIndex}
- 例如：notes['2024-05-18_36'] = '处理邮件和晨会'

**批量保存逻辑**
```javascript
function saveSlot() {
  if (!modalCat.value) return;
  const targets = dragSel.value.length > 0 ? dragSel.value : [modalIdx.value];
  targets.forEach(idx => {
    slots.value[dk.value][idx] = modalCat.value.id;
    if (modalNote.value.trim()) {
      notes.value[dk.value + '_' + idx] = modalNote.value.trim();
    }
  });
  modal.value = false;
  dragSel.value = [];
}
```

**使用场景**
- 用户选择 09:00-12:00（12 个时间块）
- 选择分类"工作"，填写备注"处理邮件和晨会"
- 点击保存后，所有 12 个块都保存相同的备注
- 悬停任意一个块都能看到这个备注

### 11.5 用户体验改进

**改进前的问题**
- 用户无法直观看到备注内容
- 重新编辑时备注信息丢失
- 需要额外操作才能查看备注

**改进后的优势**
- 悬停即可查看备注，无需点击
- 点击自动回显，编辑更便捷
- 批量备注共享，提高录入效率
- 视觉反馈清晰，用户体验流畅

---

**文档版本**: v1.1  
**最后更新**: 2024-05-19
