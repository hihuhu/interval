# 管理员账号管理与忘记密码 SDD

## 1. Overview

本设计用于给 Interval 增加轻量管理员账号管理能力。管理员账号只访问管理页面，不拥有时间格和统计功能；普通用户继续访问时间格与统计。

本阶段目标：

- 系统没有管理员时，默认创建 `admin / 123456`。
- 默认管理员首次登录必须修改密码。
- 管理员可以查看普通用户列表、用户活跃度、账号状态。
- 管理员可以重置普通用户密码。
- 管理员可以禁用或启用普通用户账号。
- 登录页提供“忘记密码”入口，当前版本提示联系管理员重置。

不在本阶段范围：

- 邮箱/短信验证码找回密码。
- 复杂角色权限系统。
- 管理员管理自己的时间记录。
- 删除用户及级联删除用户业务数据。
- 用户数据跨账号查看或编辑。

## 2. Account Model

`users` 表建议新增字段：

| Field | Type | Description |
| --- | --- | --- |
| `account_type` | enum/string | `USER` 或 `ADMIN`。管理员不进入时间格和统计。 |
| `status` | enum/string | `ACTIVE` 或 `DISABLED`。禁用后不可登录和访问 API。 |
| `created_at` | datetime | 注册或初始化创建时间。 |
| `last_login_at` | datetime nullable | 最近一次登录成功时间。 |
| `last_active_at` | datetime nullable | 最近一次业务活跃时间。 |
| `must_change_password` | boolean | 是否必须在登录后修改密码。 |
| `password_updated_at` | datetime nullable | 最近一次密码修改时间。 |

默认管理员创建规则：

- 应用启动时检查是否存在 `account_type = ADMIN` 的用户。
- 如果不存在，创建用户名 `admin`、初始密码 `123456` 的管理员。
- 初始管理员必须设置 `must_change_password = true`。
- 生产环境后续可以通过配置覆盖默认用户名和密码，但原型阶段按 `admin / 123456` 设计。

## 3. Login Routing

登录成功后按账号类型和密码状态分流：

| Condition | Destination |
| --- | --- |
| `must_change_password = true` | `/change-password` |
| `account_type = ADMIN` | `/admin` |
| `account_type = USER` | `/time-grid` |

访问控制：

- 管理员访问 `/time-grid` 或 `/stats` 时重定向到 `/admin`。
- 普通用户访问 `/admin` 时返回 403 页面。
- 禁用账号登录返回统一错误，不暴露过多账号状态细节。
- 禁用账号已有 JWT 访问受保护 API 时返回 403，并清理前端登录态。

## 4. Admin Dashboard

管理员首页展示以下指标：

| Metric | Definition | Data Source |
| --- | --- | --- |
| 总用户 | `account_type = USER` 的用户数 | `users` |
| 启用账号 | 普通用户中 `status = ACTIVE` 的数量 | `users` |
| 禁用账号 | 普通用户中 `status = DISABLED` 的数量 | `users` |
| 今日活跃 | 今日有登录或业务活动的普通用户数 | `users.last_login_at` + activity log |
| 近 7 日活跃 | 最近 7 天有登录或业务活动的普通用户数 | `users.last_login_at` + activity log |

用户列表字段：

- 用户名
- 状态
- 注册时间
- 最近登录时间
- 最近活跃时间
- 近 7 日时间记录数
- 是否需要修改密码

## 5. Activity And Audit Data

为了支持活跃度和后续审计，需要记录两类数据。

### 5.1 Lightweight User Activity

用于仪表盘和用户列表的聚合字段：

- 登录成功时更新 `users.last_login_at`。
- 用户执行业务 API 时更新 `users.last_active_at`。
- 业务 API 包括：
  - 查询/创建/更新/删除分类。
  - 查询/创建/更新/删除时间记录。
  - 查询统计数据。

为了避免每次查询都写库，`last_active_at` 可以按分钟节流更新：同一用户 60 秒内只更新一次。

### 5.2 Admin Audit Log

建议新增 `admin_audit_logs` 表，记录管理员操作：

| Field | Type | Description |
| --- | --- | --- |
| `id` | bigint | 主键。 |
| `admin_user_id` | bigint | 执行操作的管理员 ID。 |
| `target_user_id` | bigint nullable | 被操作用户 ID。 |
| `action` | string | `RESET_PASSWORD`、`DISABLE_USER`、`ENABLE_USER`、`CHANGE_OWN_PASSWORD`。 |
| `created_at` | datetime | 操作时间。 |
| `ip_address` | string nullable | 请求来源 IP。 |
| `user_agent` | string nullable | 请求 User-Agent。 |
| `metadata_json` | text nullable | 非敏感补充信息。不得写入明文密码。 |

注意：

- 重置密码时只记录操作发生，不记录临时密码明文。
- 管理员查看用户列表不需要每次写审计日志。
- 禁用、启用、重置密码必须写审计日志。

## 6. Password Reset

管理员重置用户密码流程：

1. 管理员打开用户详情。
2. 选择“自动生成”或“手动设置”临时密码。
3. 后端保存新密码哈希。
4. 设置目标用户 `must_change_password = true`。
5. 返回临时密码给管理员，只在本次响应显示。
6. 写入 `admin_audit_logs`。

用户使用临时密码登录后：

- 如果 `must_change_password = true`，只能进入修改密码页。
- 修改成功后设置 `must_change_password = false`。
- 更新 `password_updated_at`。

## 7. Disable And Enable

禁用账号：

- 仅允许管理员禁用普通用户。
- 禁用后 `status = DISABLED`。
- 用户不能登录。
- 用户已有 JWT 访问 API 时返回 403。
- 用户数据保留，不删除分类、时间记录和统计数据。
- 写入 `admin_audit_logs`。

启用账号：

- 将 `status` 改为 `ACTIVE`。
- 不自动重置密码。
- 写入 `admin_audit_logs`。

## 8. API Contract

管理员 API 均要求 JWT 且 `account_type = ADMIN`。

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/admin/dashboard` | 获取账号仪表盘指标。 |
| `GET` | `/api/admin/users` | 查询普通用户列表，支持搜索和状态筛选。 |
| `GET` | `/api/admin/users/{userId}` | 获取用户详情与活跃趋势。 |
| `POST` | `/api/admin/users/{userId}/reset-password` | 重置用户密码。 |
| `POST` | `/api/admin/users/{userId}/disable` | 禁用用户。 |
| `POST` | `/api/admin/users/{userId}/enable` | 启用用户。 |
| `POST` | `/api/auth/change-password` | 当前登录用户修改密码。 |

重置密码请求：

```json
{
  "mode": "AUTO",
  "temporaryPassword": null
}
```

手动设置：

```json
{
  "mode": "MANUAL",
  "temporaryPassword": "Temp@2026"
}
```

重置密码响应：

```json
{
  "temporaryPassword": "Tmp-ABCD-2026",
  "mustChangePassword": true
}
```

## 9. Frontend UX

登录页：

- 保持当前 Interval 登录页视觉：浅渐变背景、玻璃卡片、靛蓝主色。
- 管理员 `admin / 123456` 首次登录后进入修改密码页。
- 普通用户登录后进入时间格。
- 管理员登录后进入管理页。

管理页：

- 使用当前 `AppLayout` 顶部导航样式。
- 管理员顶部只显示“管理”，不显示“时间格”和“统计”。
- 右上角点击用户名展开菜单，菜单中包含“退出登录”。
- 禁用状态必须使用红色状态标签和红色禁用按钮。
- 启用状态必须使用绿色状态标签和绿色启用按钮。
- 重置密码弹窗使用分段控件选择“自动生成 / 手动设置”，不使用原生下拉。

### 9.1 Self-Service Password Change

- 右上角用户名菜单提供“修改密码”入口，和“退出登录”放在同一个下拉菜单中。
- 普通用户和管理员都可以主动修改自己的密码。
- 修改密码使用弹窗完成，不跳转离开当前工作页面。
- 弹窗包含当前密码、新密码、确认新密码、密码规则提示、错误提示和成功反馈。
- 弹窗视觉沿用当前玻璃面板和管理页弹窗风格：遮罩、白色面板、8px modal 圆角、清晰分区、主按钮渐变样式。
- 提交使用现有 `POST /api/auth/change-password`，前端通过 `authService.changePassword` 调用，不直接访问 Axios。

## 10. Testing Strategy

后端测试：

- 默认管理员不存在时会创建 `admin`。
- 默认管理员首次登录返回 `mustChangePassword = true`。
- 管理员不能访问普通用户业务 API。
- 普通用户不能访问管理员 API。
- 禁用账号不能登录。
- 禁用账号携带旧 JWT 访问 API 返回 403。
- 重置密码后目标用户必须改密。
- 禁用、启用、重置密码写入审计日志。

前端测试：

- 登录页按账号类型跳转。
- `mustChangePassword` 用户跳转修改密码页。
- 管理员页只显示管理导航。
- 用户名菜单点击后显示退出登录。
- 用户列表显示禁用/启用状态。
- 重置密码弹窗可以切换自动生成和手动设置。
- 禁用/启用按钮文案和状态色正确。

## 11. Acceptance Criteria

- 系统首次启动后可使用 `admin / 123456` 登录。
- admin 首次登录必须修改密码。
- 管理员只能进入 `/admin`。
- 普通用户不能进入 `/admin`。
- 管理员可查看普通用户列表和活跃指标。
- 管理员可重置用户密码，并让用户下次登录强制改密。
- 管理员可禁用和启用普通用户。
- 仪表盘指标有明确数据来源。
- 管理员关键操作有审计日志。
