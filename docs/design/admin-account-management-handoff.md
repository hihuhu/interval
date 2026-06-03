# 管理员账号管理交接摘要

## 已完成

- 原型：`docs/prototypes/admin-account-management.html`
- 原型索引：`docs/prototypes/README.md`
- 需求/设计文档：`docs/design/sdd-admin-account-management.md`
- 实现计划：`docs/superpowers/plans/2026-06-03-admin-account-management.md`

## 已确认产品决策

- 系统没有管理员时默认创建 `admin / 123456`。
- 默认管理员首次登录必须修改密码。
- 管理员账号只访问管理页面，不访问时间格和统计。
- 普通用户继续访问时间格和统计。
- 当前没有邮箱/短信，忘记密码先做“联系管理员重置密码”。
- 管理员可查看用户列表、活跃度、重置密码、禁用/启用账号。
- 管理员重置密码后，用户下次登录必须修改密码。
- 禁用账号保留数据，但不能登录；已有 JWT 访问也必须被拒绝。

## 原型交互重点

- 登录页保持现有 Interval 视觉体系。
- 普通用户登录后进入时间格视图。
- 管理员登录后进入管理页。
- 右上角用户名点击后显示下拉菜单，菜单里有退出登录。
- 管理员顶部只显示“管理”，不显示“时间格 / 统计”。
- 用户列表操作按钮必须显示文字：重置、禁用、启用。
- 禁用状态在表格和右侧详情都使用红色 UI。
- 重置密码弹窗用分段控件选择“自动生成 / 手动设置”，不用原生下拉。

## 统计和日志数据要求

活跃度字段：

- `users.last_login_at`
- `users.last_active_at`

审计日志表：

- `admin_audit_logs`
- 记录 `RESET_PASSWORD`、`DISABLE_USER`、`ENABLE_USER`、`CHANGE_OWN_PASSWORD`
- 不记录临时密码明文。

仪表盘指标：

- 总用户
- 启用账号
- 禁用账号
- 今日活跃
- 近 7 日活跃

## 当前代码差异

当前 `SeedDataInitializer` 已经有 admin 种子逻辑，但存在差异：

- 当前默认密码是 `123ABCdef*`，目标是 `123456`。
- 当前会给 admin 创建默认分类，目标是管理员不产生时间格/统计数据。
- 当前 `User` 没有账号类型、状态、强制改密、最近登录、最近活跃字段。
- 当前 `LoginResponseDto` 只返回 token 和 username。
- 当前前端 `AppLayout` 是用户名文本 + 退出按钮，目标是用户名下拉菜单。

## 新窗口建议起点

打开并执行：

`docs/superpowers/plans/2026-06-03-admin-account-management.md`

建议先从后端任务 1 开始：

1. 扩展 `User` 模型。
2. 修正默认管理员创建逻辑。
3. 移除管理员默认分类创建。
4. 写并跑 `SeedDataInitializerTest`。

完成后再做登录响应、改密、禁用账号访问拦截和管理员 API。
