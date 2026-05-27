# 原型文件说明

> 本目录用于保存 Interval 的交互与视觉原型。原型主要用于确认体验方向，不代表正式前后端实现。

---

## Time Grid v2 UI iteration note

The latest formal Vue UI iteration for the time grid is documented here:

- `../design/time-grid-v2-ui-iteration-2026-05-27.md`

Key decisions:

- Empty time cells should stay visually blank.
- Occupied cells should show only compact category labels.
- Selected cells should use light fill plus clear inset outline, not heavy overlays, corner dots, or bottom underlines.
- The right-side category picker should use the prototype-style visible category list, not a native dropdown.

---

## 当前推荐查看顺序

1. `modern-time-grid-v2.html` — 当前时间格主原型。
2. `stats-category-duration.html` — 分类耗时统计页面原型。
3. `login.html` — 早期登录页面原型，仅作登录视觉参考。

---

## 原型文件清单

### ✅ modern-time-grid-v2.html

**状态**：当前主时间格原型  
**用途**：验证 96 格时间记录、日期切换、分类管理、连续选择、右侧选择面板和已登记块擦除等核心体验。

**主要功能**：

- 登录 / 注册演示入口。
- 96 个 15 分钟时间格。
- 日期选择器。
- 分类管理面板。
- 单格与连续范围选择。
- 右侧选择详情面板。
- 对已登记选中块执行擦除。
- 本地模拟数据，不连接真实 API。

**查看方法**：

1. 双击打开 `modern-time-grid-v2.html`。
2. 使用任意登录信息进入演示页面。
3. 选择时间格并观察右侧面板。
4. 选择已登记时间块后测试擦除按钮。

---

### ✅ stats-category-duration.html

**状态**：当前统计页面原型  
**用途**：验证“统计各分类花了多长时间”的页面信息架构、视觉层级和基础交互。

**主要功能**：

- 今日 / 本周 / 本月 / 自定义统计范围切换。
- 总已记录时间、时间块数量、记录率、未记录时间汇总。
- 分类耗时列表。
- 分类占比进度条。
- 右侧环形结构概览。
- 点击分类查看详情。
- 已归档分类展示。
- 空状态预览。
- 本地模拟数据，不连接真实 API。

**查看方法**：

1. 双击打开 `stats-category-duration.html`。
2. 点击“今日 / 本周 / 本月 / 自定义”查看不同统计范围。
3. 点击分类行查看右侧详情变化。
4. 点击“查看空状态”观察无记录时的页面效果。

**设计文档**：

- `../design/sdd-category-duration-stats.md`

---

### ✅ login.html

**状态**：早期登录视觉原型  
**用途**：保留用于参考登录界面的极简视觉和流程。

**主要功能**：

- 用户登录界面。
- 用户名和密码必填校验。
- 原型阶段输入任意凭据即可进入演示页。
- Token 写入 `localStorage` 的早期演示。

---

### ✅ time-grid-simple.html

**状态**：早期简化占位原型  
**用途**：早期用于测试登录跳转与页面骨架，目前不作为主要体验参考。

**说明**：

- 正式时间格体验请查看 `modern-time-grid-v2.html`。
- 本文件保留用于追溯早期登录流程验证。

---

### ⚠️ time-grid-v1.html

**状态**：已弃用 / 历史损坏文件  
**说明**：该文件曾被错误覆盖为 TodoMVC 示例，不再作为有效原型参考。

后续如需进一步清理，可在确认无引用后删除该文件。

---

## 已删除原型

### modern-time-grid.html

**状态**：已删除  
**原因**：该版本已被 `modern-time-grid-v2.html` 取代，继续保留容易造成误用。

---

## 当前原型到正式实现的规划

### 1. 时间格主流程

当前主参考原型：`modern-time-grid-v2.html`

正式实现状态（2026-05-27）：

- `TimeGridView` 已按 v2 原型重构为工作台布局：顶部 Hero、指标卡、日期切换、左侧时间网格、右侧选择编辑和分类管理。
- 正式时间网格已采用原型的小时行结构：24 行，每行 4 个 15 分钟块。
- 已实现拖拽连续选择、单击跳选、再次单击取消、拖选后继续点选追加。
- 已实现右侧选择面板、备注回显、混合分类/混合备注提示、批量保存和已登记块擦除。
- 后端已新增批量保存和批量删除 TimeSlot API。

下一步建议：

- 继续细化右侧选择编辑面板的视觉密度、按钮状态和表单反馈。
- 用真实浏览器补做保存/覆盖/擦除的端到端冒烟。
- 主流程稳定后，再回到分类耗时统计功能。

### 2. 分类耗时统计

当前主参考原型：`stats-category-duration.html`

下一步建议：

- 确认统计页面视觉和交互。
- 批准或修订 `docs/design/sdd-category-duration-stats.md`。
- 按 SDD 顺序先写后端 JUnit 5 测试，再实现统计 API。
- 后端完成后，先写前端 Vitest 测试，再实现 `StatsView`、service、store 和路由入口。

---

## 原型维护规则

- 新原型应优先以当前主视觉风格为基础，避免重复创建多套视觉语言。
- 弃用原型应在本文件中标记状态；确认无引用后可删除。
- 原型只用于验证交互和信息架构，不应替代正式 Vue 组件实现。
- 正式功能实现仍需遵循 SDD：先更新 `docs/design/`，再写测试，最后实现。

---

## 2026-05 原型优化总结

### 1. 时间格原型（`modern-time-grid-v2.html`）

本轮完成了两类改进：

- **视觉升级**
  - 统一为更接近正式产品的玻璃感卡片、柔和渐变和更清晰的信息层级。
  - 增加顶部 Hero、指标卡、强化右侧编辑面板与网格区域的视觉区分。
  - 优化按钮、标签、悬停态、选中态和空状态表现。

- **多选交互修复**
  - 修复“先选多个块，再混合点选不同已登记块时，右侧分类/备注显示错误”的问题。
  - 正式 Vue 页面已修复真实浏览器 `pointerdown -> pointerup -> click` 事件序列导致的拖选后点选丢失问题。
  - 拖选开始时不再立即清空旧选择；只有确认拖出连续范围后才替换选择集。
  - 网格层通过坐标识别当前时间块，降低真实拖拽时 `pointerenter` 不稳定带来的漏选风险。
  - 右侧面板现在明确区分三种状态：
    1. **统一分类 / 统一备注**：自动回显。
    2. **混合分类**：不再错误保留旧分类，提示用户重新指定统一分类。
    3. **混合备注**：不再错误回显旧备注，提示“如不修改则保留原备注”。
  - 保存逻辑更新为：
    - 未主动编辑备注：仅更新分类，原备注保留。
    - 主动编辑备注：批量覆盖所有选中块备注。
    - 主动清空备注并保存：删除所有选中块备注。

### 2. 统计原型（`stats-category-duration.html`）

- 完成统计页整体视觉重构，使其更像正式产品页面，而不是纯演示稿。
- 增加更明确的页面层级：
  - Hero 区
  - 统计摘要区
  - 分类列表区
  - 圆环结构概览
  - 分类详情
  - 趋势占位模块
- 保留现有静态数据结构，方便后续直接映射真实统计 API。

### 3. 换设备继续开发时建议优先做的事

1. 先确认并评审两个原型是否作为正式 UI 基准。
2. 将时间格右侧面板的“混合多选状态规则”写入正式前端设计文档。
3. 统计页按 SDD 顺序推进：
   - 先补 `docs/design/sdd-category-duration-stats.md`
   - 再写后端统计 API 测试与实现
   - 最后接前端 `StatsView / service / store`
4. 正式 Vue 实现时，优先抽离通用 UI：
   - 卡片
   - 指标卡
   - 标签 / Chip
   - 侧栏编辑面板
   - 空状态
