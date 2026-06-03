# Time Grid UI Cleanup 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:executing-plans 在当前会话中执行此计划。步骤使用复选框语法来跟踪进度。

**目标：** 清理登录页、顶部导航、日期工具栏、时间网格视觉和右侧编辑区，让 Time Grid 更安静、更像可长期使用的记录工具。

**架构：** 保持现有 Vue 组件边界不做大拆分。`TimeGridView.vue` 负责页面布局、日期控制和分类管理弹窗状态；`SelectionEditorPanel.vue` 专注当前选择的编辑；`CategoryManagerPanel.vue` 复用为弹窗主体；`TimeGrid.vue` 与 `TimeSlotCell.vue` 只调整网格视觉和交互提示。

**技术栈：** Vue 3、TypeScript、Vite、Pinia、Vue Test Utils、Vitest、Lucide Vue。

---

## 文件结构

- 修改：`interval-client/src/views/LoginView.vue`
  - 去掉开发账号提示和默认预填账号密码。
- 修改：`interval-client/src/components/AppLayout.vue`
  - 移除顶部“分类”导航项。
- 修改：`interval-client/src/views/TimeGridView.vue`
  - 重做日期工具栏；将分类管理移入弹窗；连接弹窗开关状态。
- 修改：`interval-client/src/components/SelectionEditorPanel.vue`
  - 重做为更安静的当前选择编辑面板；增加“管理分类”入口事件。
- 修改：`interval-client/src/components/CategoryManagerPanel.vue`
  - 支持作为弹窗主体显示，减少常驻面板视觉重量。
- 修改：`interval-client/src/components/TimeGrid.vue`
  - 降低网格容器、表头、提示 chip 的视觉噪音。
- 修改：`interval-client/src/components/TimeSlotCell.vue`
  - 降低空格亮度和 hover/selected 发光。
- 修改/新增测试：
  - `interval-client/src/__tests__/useAuthStore.test.ts`
  - `interval-client/src/__tests__/router.test.ts`
  - `interval-client/src/__tests__/TimeGrid.test.ts`
  - `interval-client/src/__tests__/SelectionEditorPanel.test.ts`
  - `interval-client/src/__tests__/TimeGridView.test.ts`

## 任务 1：登录页与导航清理

- [ ] 步骤 1：更新登录页测试，断言登录页不展示开发账号文案且输入框为空。
- [ ] 步骤 2：运行登录相关测试，确认测试失败。
- [ ] 步骤 3：修改 `LoginView.vue`，移除预填和开发账号提示。
- [ ] 步骤 4：更新 `AppLayout.vue`，删除“分类”导航项。
- [ ] 步骤 5：运行相关测试确认通过。

## 任务 2：日期工具栏重设计

- [ ] 步骤 1：更新 `TimeGridView` 测试，断言日期控制使用图标按钮与“今天”按钮，点击会调用日期切换并刷新数据。
- [ ] 步骤 2：运行测试确认失败。
- [ ] 步骤 3：修改 `TimeGridView.vue` 日期区域结构和样式，去掉“前一天/后一天”文字按钮。
- [ ] 步骤 4：运行测试确认通过。

## 任务 3：右侧编辑面板与分类管理弹窗

- [ ] 步骤 1：更新 `SelectionEditorPanel` 测试，断言面板提供“管理分类”入口并发出 `manageCategories` 事件。
- [ ] 步骤 2：更新 `TimeGridView` 测试，断言点击“管理分类”打开对话框，关闭按钮可关闭。
- [ ] 步骤 3：运行测试确认失败。
- [ ] 步骤 4：修改 `SelectionEditorPanel.vue`，减少提示文案，优化摘要和编辑区布局，并发出管理分类事件。
- [ ] 步骤 5：修改 `TimeGridView.vue`，将 `CategoryManagerPanel` 放入 modal/dialog，并连接关闭、创建、更新、删除逻辑。
- [ ] 步骤 6：运行测试确认通过。

## 任务 4：时间网格视觉降噪

- [ ] 步骤 1：更新 `TimeGrid` 测试，保留 96 格、已登记标签和空格无文字行为。
- [ ] 步骤 2：修改 `TimeGrid.vue` 与 `TimeSlotCell.vue` 样式，去掉强渐变、强阴影和晃眼 hover，保留清晰选中态。
- [ ] 步骤 3：运行 `TimeGrid` 测试确认行为未变。

## 任务 5：验证与记录

- [ ] 步骤 1：运行 `npm run test -- src/__tests__/TimeGridView.test.ts src/__tests__/SelectionEditorPanel.test.ts src/__tests__/TimeGrid.test.ts src/__tests__/router.test.ts src/__tests__/useAuthStore.test.ts`。
- [ ] 步骤 2：运行 `npm run build`。
- [ ] 步骤 3：用浏览器验证登录页、自动/手动登录、日期切换、Time Grid、分类弹窗。
- [ ] 步骤 4：更新 `PROJECT_LOG.md` 记录本次调整与验证结果。
