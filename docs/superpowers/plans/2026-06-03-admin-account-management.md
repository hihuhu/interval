# 管理员账号管理实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现默认管理员、首次改密、管理员账号管理、忘记密码提示、禁用/启用账号、重置密码和活跃度仪表盘。

**架构：** 在现有 Spring Boot JWT 认证和 Vue 3/Pinia 前端上扩展账号类型与账号状态。后端通过 `account_type`、`status`、`must_change_password` 等字段控制登录分流和访问权限；前端新增管理员路由、管理页面和修改密码页，沿用当前 Interval 视觉体系。

**技术栈：** Spring Boot 3.3、Spring Security、Spring Data JPA、JWT、JUnit 5、Mockito、Vue 3、Vue Router、Pinia、Axios、Vitest、Vue Test Utils。

---

## 0. 参考资料

- 需求文档：`docs/design/sdd-admin-account-management.md`
- 原型：`docs/prototypes/admin-account-management.html`
- 当前后端认证：`interval-server/src/main/java/com/interval/auth`
- 当前前端认证：`interval-client/src/stores/useAuthStore.ts`

注意当前代码中已有 `SeedDataInitializer`，但它使用 `admin / 123ABCdef*`，并且会给 admin 创建默认分类。这与本需求不一致，必须在任务 1 修正为 `admin / 123456`，且管理员不创建时间格相关默认数据。

## 1. 文件结构

### 后端新增/修改

- 修改：`interval-server/src/main/java/com/interval/auth/entity/User.java`
  - 增加账号类型、状态、时间戳、强制改密字段。
- 新增：`interval-server/src/main/java/com/interval/auth/entity/AccountType.java`
  - 定义 `USER`、`ADMIN`。
- 新增：`interval-server/src/main/java/com/interval/auth/entity/UserStatus.java`
  - 定义 `ACTIVE`、`DISABLED`。
- 修改：`interval-server/src/main/java/com/interval/auth/config/SeedDataInitializer.java`
  - 创建默认管理员，移除给管理员创建默认分类。
- 修改：`interval-server/src/main/java/com/interval/auth/dto/LoginResponseDto.java`
  - 返回 `accountType`、`mustChangePassword`。
- 新增：`interval-server/src/main/java/com/interval/auth/dto/ChangePasswordRequest.java`
  - 当前密码、新密码。
- 修改：`interval-server/src/main/java/com/interval/auth/service/AuthService.java`
- 修改：`interval-server/src/main/java/com/interval/auth/service/AuthServiceImpl.java`
  - 登录状态检查、登录时间、改密逻辑。
- 修改：`interval-server/src/main/java/com/interval/auth/security/AuthenticatedUser.java`
  - 增加 `accountType`、`status`。
- 修改：`interval-server/src/main/java/com/interval/auth/security/JwtAuthenticationFilter.java`
  - 拒绝禁用账号，注入账号类型。
- 修改：`interval-server/src/main/java/com/interval/auth/config/SecurityConfig.java`
  - 放行忘记密码说明不需要 API；管理员 API 仍需认证。
- 新增：`interval-server/src/main/java/com/interval/admin/entity/AdminAuditLog.java`
  - 管理员审计日志。
- 新增：`interval-server/src/main/java/com/interval/admin/entity/AdminAuditAction.java`
  - `RESET_PASSWORD`、`DISABLE_USER`、`ENABLE_USER`、`CHANGE_OWN_PASSWORD`。
- 新增：`interval-server/src/main/java/com/interval/admin/repository/AdminAuditLogRepository.java`
- 新增：`interval-server/src/main/java/com/interval/admin/dto/AdminDashboardDto.java`
- 新增：`interval-server/src/main/java/com/interval/admin/dto/AdminUserListItemDto.java`
- 新增：`interval-server/src/main/java/com/interval/admin/dto/AdminUserDetailDto.java`
- 新增：`interval-server/src/main/java/com/interval/admin/dto/ResetPasswordRequest.java`
- 新增：`interval-server/src/main/java/com/interval/admin/dto/ResetPasswordResponseDto.java`
- 新增：`interval-server/src/main/java/com/interval/admin/service/AdminAccountService.java`
- 新增：`interval-server/src/main/java/com/interval/admin/service/AdminAccountServiceImpl.java`
- 新增：`interval-server/src/main/java/com/interval/admin/controller/AdminAccountController.java`
- 修改：`interval-server/src/main/java/com/interval/timeslot/service/TimeSlotServiceImpl.java`
  - 用户写入时间记录后更新 `lastActiveAt`，或通过专门 activity service 更新。
- 修改：`interval-server/src/main/java/com/interval/category/service/CategoryServiceImpl.java`
  - 用户分类操作后更新 `lastActiveAt`。
- 修改：`interval-server/src/main/java/com/interval/stats/service/StatsServiceImpl.java`
  - 查询统计可按需求更新 `lastActiveAt`。

### 后端测试

- 修改：`interval-server/src/test/java/com/interval/auth/AuthServiceTest.java`
- 新增：`interval-server/src/test/java/com/interval/auth/SeedDataInitializerTest.java`
- 新增：`interval-server/src/test/java/com/interval/auth/ChangePasswordTest.java`
- 新增：`interval-server/src/test/java/com/interval/auth/JwtAccountStatusTest.java`
- 新增：`interval-server/src/test/java/com/interval/admin/AdminAccountServiceTest.java`
- 新增：`interval-server/src/test/java/com/interval/admin/AdminAccountControllerTest.java`

### 前端新增/修改

- 修改：`interval-client/src/types/auth.ts`
  - 增加 `accountType`、`mustChangePassword`、改密请求类型。
- 修改：`interval-client/src/services/authService.ts`
  - 增加 `changePassword`。
- 新增：`interval-client/src/types/admin.ts`
- 新增：`interval-client/src/services/adminService.ts`
- 新增：`interval-client/src/stores/useAdminStore.ts`
- 修改：`interval-client/src/stores/useAuthStore.ts`
  - 保存账号类型和强制改密状态。
- 修改：`interval-client/src/router/index.ts`
  - 增加 `/admin`、`/change-password`、权限守卫。
- 修改：`interval-client/src/components/AppLayout.vue`
  - 用户名下拉退出；支持普通用户导航和管理员导航。
- 新增：`interval-client/src/views/ChangePasswordView.vue`
- 新增：`interval-client/src/views/AdminAccountView.vue`
- 修改：`interval-client/src/views/LoginView.vue`
  - 增加忘记密码入口，按登录响应分流。
- 新增：`interval-client/src/views/ForgotPasswordView.vue`
  - 当前版本只提示联系管理员。

### 前端测试

- 修改：`interval-client/src/__tests__/useAuthStore.test.ts`
- 修改：`interval-client/src/__tests__/router.test.ts`
- 修改：`interval-client/src/__tests__/AppLayout.test.ts`
- 新增：`interval-client/src/__tests__/AdminAccountView.test.ts`
- 新增：`interval-client/src/__tests__/ChangePasswordView.test.ts`
- 新增：`interval-client/src/__tests__/adminService.test.ts`

---

## 2. 后端任务

### 任务 1：扩展用户模型和默认管理员

**文件：**

- 修改：`interval-server/src/main/java/com/interval/auth/entity/User.java`
- 创建：`interval-server/src/main/java/com/interval/auth/entity/AccountType.java`
- 创建：`interval-server/src/main/java/com/interval/auth/entity/UserStatus.java`
- 修改：`interval-server/src/main/java/com/interval/auth/repository/UserRepository.java`
- 修改：`interval-server/src/main/java/com/interval/auth/config/SeedDataInitializer.java`
- 测试：`interval-server/src/test/java/com/interval/auth/SeedDataInitializerTest.java`

- [ ] **步骤 1：编写失败测试：默认管理员不存在时创建 `admin / 123456`**

测试要断言：

```java
assertEquals("admin", saved.getUsername());
assertEquals(AccountType.ADMIN, saved.getAccountType());
assertEquals(UserStatus.ACTIVE, saved.getStatus());
assertTrue(saved.isMustChangePassword());
verify(categoryRepository, never()).save(any());
```

- [ ] **步骤 2：编写失败测试：已有管理员时不覆盖密码**

测试要断言 `passwordEncoder.encode("123456")` 不被重复调用，避免每次启动重置管理员密码。

- [ ] **步骤 3：实现枚举和字段**

`User.java` 增加：

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private AccountType accountType = AccountType.USER;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private UserStatus status = UserStatus.ACTIVE;

@Column(nullable = false)
private boolean mustChangePassword = false;

@Column(nullable = false)
private Instant createdAt = Instant.now();

private Instant lastLoginAt;
private Instant lastActiveAt;
private Instant passwordUpdatedAt;
```

- [ ] **步骤 4：调整 `SeedDataInitializer`**

行为：

- 如果不存在管理员，创建 `admin / 123456`。
- 设置 `accountType = ADMIN`、`status = ACTIVE`、`mustChangePassword = true`。
- 不为管理员创建默认分类。
- 保留普通用户注册后的分类逻辑不在此任务处理。

- [ ] **步骤 5：运行后端测试**

运行：

```powershell
cd interval-server
./gradlew test --tests "*SeedDataInitializerTest"
```

预期：新增测试通过。

### 任务 2：登录响应、禁用账号和首次改密

**文件：**

- 修改：`interval-server/src/main/java/com/interval/auth/dto/LoginResponseDto.java`
- 创建：`interval-server/src/main/java/com/interval/auth/dto/ChangePasswordRequest.java`
- 修改：`interval-server/src/main/java/com/interval/auth/service/AuthService.java`
- 修改：`interval-server/src/main/java/com/interval/auth/service/AuthServiceImpl.java`
- 修改：`interval-server/src/main/java/com/interval/auth/controller/AuthController.java`
- 测试：`interval-server/src/test/java/com/interval/auth/AuthServiceTest.java`
- 测试：`interval-server/src/test/java/com/interval/auth/ChangePasswordTest.java`

- [ ] **步骤 1：扩展登录成功测试**

断言 `LoginResponseDto` 包含：

```java
assertEquals("USER", response.accountType());
assertFalse(response.mustChangePassword());
```

- [ ] **步骤 2：增加禁用账号登录失败测试**

构造 `UserStatus.DISABLED` 用户，调用 `login` 应抛出 `AuthenticationFailedException`，消息使用 `"Invalid username or password"` 或 `"Account is disabled"`。推荐统一使用 `"Invalid username or password"`，减少账号枚举风险。

- [ ] **步骤 3：增加登录更新时间测试**

登录成功后断言 `user.setLastLoginAt(...)` 被保存。

- [ ] **步骤 4：增加改密测试**

覆盖：

- 当前密码错误失败。
- 新密码通过 `PasswordValidator`。
- 成功后更新 `passwordHash`、`mustChangePassword = false`、`passwordUpdatedAt`。

- [ ] **步骤 5：实现 DTO 和服务方法**

`LoginResponseDto`：

```java
public record LoginResponseDto(
    String token,
    String username,
    String accountType,
    boolean mustChangePassword
) {}
```

`ChangePasswordRequest`：

```java
public record ChangePasswordRequest(String currentPassword, String newPassword) {}
```

`AuthService` 增加：

```java
void changePassword(Long userId, String currentPassword, String newPassword);
```

- [ ] **步骤 6：实现 controller**

`POST /api/auth/change-password` 读取 `AuthenticatedUser`，调用 service。

- [ ] **步骤 7：运行后端认证测试**

运行：

```powershell
cd interval-server
./gradlew test --tests "*AuthServiceTest" --tests "*ChangePasswordTest"
```

预期：相关测试通过。

### 任务 3：JWT 注入账号状态并阻止禁用账号访问

**文件：**

- 修改：`interval-server/src/main/java/com/interval/auth/security/AuthenticatedUser.java`
- 修改：`interval-server/src/main/java/com/interval/auth/security/JwtAuthenticationFilter.java`
- 测试：`interval-server/src/test/java/com/interval/auth/JwtAccountStatusTest.java`

- [ ] **步骤 1：编写禁用账号旧 token 访问失败测试**

模拟 JWT 有效，但 repository 返回 `status = DISABLED` 的用户，期望响应 `403` 或 `401`。推荐 `403 FORBIDDEN`，因为身份存在但不可用。

- [ ] **步骤 2：修改 principal**

```java
public record AuthenticatedUser(
    Long userId,
    String username,
    AccountType accountType,
    UserStatus status
) {}
```

- [ ] **步骤 3：修改 filter 查询完整用户**

从 `existsById` 改为 `findById`，校验：

- 用户存在。
- `status = ACTIVE`。
- 注入 `accountType`。

- [ ] **步骤 4：运行测试**

```powershell
cd interval-server
./gradlew test --tests "*JwtAccountStatusTest"
```

### 任务 4：管理员审计日志

**文件：**

- 创建：`interval-server/src/main/java/com/interval/admin/entity/AdminAuditLog.java`
- 创建：`interval-server/src/main/java/com/interval/admin/entity/AdminAuditAction.java`
- 创建：`interval-server/src/main/java/com/interval/admin/repository/AdminAuditLogRepository.java`
- 测试：`interval-server/src/test/java/com/interval/admin/AdminAccountServiceTest.java`

- [ ] **步骤 1：创建审计实体测试**

服务执行重置、禁用、启用时，断言 `adminAuditLogRepository.save(...)` 被调用，并且 action 正确。

- [ ] **步骤 2：实现实体**

字段按 `docs/design/sdd-admin-account-management.md`：

- `adminUserId`
- `targetUserId`
- `action`
- `createdAt`
- `ipAddress`
- `userAgent`
- `metadataJson`

- [ ] **步骤 3：运行测试**

```powershell
cd interval-server
./gradlew test --tests "*AdminAccountServiceTest"
```

### 任务 5：管理员账号管理 API

**文件：**

- 创建：`interval-server/src/main/java/com/interval/admin/dto/AdminDashboardDto.java`
- 创建：`interval-server/src/main/java/com/interval/admin/dto/AdminUserListItemDto.java`
- 创建：`interval-server/src/main/java/com/interval/admin/dto/AdminUserDetailDto.java`
- 创建：`interval-server/src/main/java/com/interval/admin/dto/ResetPasswordRequest.java`
- 创建：`interval-server/src/main/java/com/interval/admin/dto/ResetPasswordResponseDto.java`
- 创建：`interval-server/src/main/java/com/interval/admin/service/AdminAccountService.java`
- 创建：`interval-server/src/main/java/com/interval/admin/service/AdminAccountServiceImpl.java`
- 创建：`interval-server/src/main/java/com/interval/admin/controller/AdminAccountController.java`
- 修改：`interval-server/src/main/java/com/interval/auth/repository/UserRepository.java`
- 测试：`interval-server/src/test/java/com/interval/admin/AdminAccountServiceTest.java`
- 测试：`interval-server/src/test/java/com/interval/admin/AdminAccountControllerTest.java`

- [ ] **步骤 1：编写 dashboard 测试**

构造用户数据，断言：

- 总用户不含管理员。
- 启用/禁用数量正确。
- 今日活跃和近 7 日活跃按 `lastLoginAt` 或 `lastActiveAt` 计算。

- [ ] **步骤 2：编写用户列表测试**

断言：

- 不返回管理员。
- 支持 `keyword`。
- 支持 `status`。

- [ ] **步骤 3：编写重置密码测试**

断言：

- 自动生成临时密码。
- 保存 hash。
- 设置 `mustChangePassword = true`。
- 不把明文密码写入审计日志。

- [ ] **步骤 4：编写禁用/启用测试**

断言只能操作普通用户，不能禁用管理员。

- [ ] **步骤 5：实现 service 和 controller**

API：

```text
GET  /api/admin/dashboard
GET  /api/admin/users?keyword=&status=
GET  /api/admin/users/{userId}
POST /api/admin/users/{userId}/reset-password
POST /api/admin/users/{userId}/disable
POST /api/admin/users/{userId}/enable
```

管理员权限检查：

```java
private void requireAdmin(AuthenticatedUser user) {
    if (user.accountType() != AccountType.ADMIN) {
        throw new AccessDeniedException("Admin access required");
    }
}
```

- [ ] **步骤 6：运行管理员测试**

```powershell
cd interval-server
./gradlew test --tests "*AdminAccountServiceTest" --tests "*AdminAccountControllerTest"
```

### 任务 6：用户活跃度更新

**文件：**

- 修改：`interval-server/src/main/java/com/interval/auth/repository/UserRepository.java`
- 创建：`interval-server/src/main/java/com/interval/auth/service/UserActivityService.java`
- 创建：`interval-server/src/main/java/com/interval/auth/service/UserActivityServiceImpl.java`
- 修改：`interval-server/src/main/java/com/interval/category/service/CategoryServiceImpl.java`
- 修改：`interval-server/src/main/java/com/interval/timeslot/service/TimeSlotServiceImpl.java`
- 修改：`interval-server/src/main/java/com/interval/stats/service/StatsServiceImpl.java`
- 测试：相关 service 测试。

- [ ] **步骤 1：编写 activity service 测试**

同一用户 60 秒内重复调用只保存一次，超过 60 秒会更新 `lastActiveAt`。

- [ ] **步骤 2：实现 `UserActivityService`**

接口：

```java
void markActive(Long userId);
```

- [ ] **步骤 3：接入业务 service**

在分类、时间记录、统计查询成功路径调用 `markActive(userId)`。

- [ ] **步骤 4：运行相关测试**

```powershell
cd interval-server
./gradlew test --tests "*CategoryServiceTest" --tests "*TimeSlotServiceTest" --tests "*StatsServiceTest"
```

---

## 3. 前端任务

### 任务 7：扩展 auth 类型、service 和 store

**文件：**

- 修改：`interval-client/src/types/auth.ts`
- 修改：`interval-client/src/services/authService.ts`
- 修改：`interval-client/src/stores/useAuthStore.ts`
- 测试：`interval-client/src/__tests__/useAuthStore.test.ts`
- 测试：`interval-client/src/__tests__/authService.test.ts`（如果当前没有则创建）

- [ ] **步骤 1：编写 store 测试**

覆盖：

- 登录保存 `token`、`username`、`accountType`、`mustChangePassword`。
- `restoreSession` 恢复账号类型。
- `logout` 清理所有 auth key。

- [ ] **步骤 2：实现类型**

```ts
export type AccountType = 'USER' | 'ADMIN';

export interface LoginResponseDto {
  token: string;
  username: string;
  accountType: AccountType;
  mustChangePassword: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
```

- [ ] **步骤 3：实现 service 和 store**

增加 localStorage key：

- `interval.auth.accountType`
- `interval.auth.mustChangePassword`

- [ ] **步骤 4：运行前端 auth 测试**

```powershell
cd interval-client
npm run test -- useAuthStore authService
```

### 任务 8：路由分流和改密页

**文件：**

- 修改：`interval-client/src/router/index.ts`
- 创建：`interval-client/src/views/ChangePasswordView.vue`
- 修改：`interval-client/src/views/LoginView.vue`
- 创建：`interval-client/src/views/ForgotPasswordView.vue`
- 测试：`interval-client/src/__tests__/router.test.ts`
- 测试：`interval-client/src/__tests__/ChangePasswordView.test.ts`
- 测试：`interval-client/src/__tests__/LoginView.test.ts`

- [ ] **步骤 1：编写路由测试**

覆盖：

- 管理员登录态访问 `/login` 重定向 `/admin`。
- 普通用户登录态访问 `/login` 重定向 `/time-grid`。
- `mustChangePassword` 用户访问非 `/change-password` 受保护页面时重定向 `/change-password`。
- 普通用户访问 `/admin` 重定向 403 或 `/time-grid`。按 SDD 使用 403。
- 管理员访问 `/time-grid` 或 `/stats` 重定向 `/admin`。

- [ ] **步骤 2：实现路由**

新增：

```ts
{ path: '/forgot-password', name: 'forgotPassword', component: () => import('@/views/ForgotPasswordView.vue'), meta: { publicOnly: true } }
{ path: '/change-password', name: 'changePassword', component: () => import('@/views/ChangePasswordView.vue'), meta: { requiresAuth: true } }
{ path: '/admin', name: 'admin', component: () => import('@/views/AdminAccountView.vue'), meta: { requiresAuth: true, adminOnly: true } }
```

- [ ] **步骤 3：实现登录页分流**

登录成功后：

```ts
if (auth.mustChangePassword) router.push('/change-password');
else if (auth.accountType === 'ADMIN') router.push('/admin');
else router.push(redirect || '/time-grid');
```

- [ ] **步骤 4：实现忘记密码页**

静态说明：当前版本请联系管理员重置密码。

- [ ] **步骤 5：运行测试**

```powershell
cd interval-client
npm run test -- router LoginView ChangePasswordView
```

### 任务 9：AppLayout 用户名下拉和管理员导航

**文件：**

- 修改：`interval-client/src/components/AppLayout.vue`
- 测试：`interval-client/src/__tests__/AppLayout.test.ts`

- [ ] **步骤 1：编写组件测试**

覆盖：

- 普通用户显示“时间格 / 统计”。
- 管理员只显示“管理”。
- 点击用户名显示下拉菜单。
- 点击下拉里的“退出登录”触发 `logout`。

- [ ] **步骤 2：修改 props**

```ts
interface AppLayoutProps {
  username: string;
  activeRoute?: 'timeGrid' | 'stats' | 'admin';
  accountType?: 'USER' | 'ADMIN';
}
```

- [ ] **步骤 3：实现下拉菜单**

保留当前视觉样式，参考原型的右上角用户名菜单。

- [ ] **步骤 4：运行测试**

```powershell
cd interval-client
npm run test -- AppLayout
```

### 任务 10：管理员页面、service 和 store

**文件：**

- 创建：`interval-client/src/types/admin.ts`
- 创建：`interval-client/src/services/adminService.ts`
- 创建：`interval-client/src/stores/useAdminStore.ts`
- 创建：`interval-client/src/views/AdminAccountView.vue`
- 测试：`interval-client/src/__tests__/adminService.test.ts`
- 测试：`interval-client/src/__tests__/AdminAccountView.test.ts`

- [ ] **步骤 1：编写 service 测试**

覆盖：

- `fetchDashboard` 调用 `/api/admin/dashboard`。
- `fetchUsers` 携带搜索和状态筛选参数。
- `resetPassword` 调用正确端点。
- `disableUser` / `enableUser` 调用正确端点。

- [ ] **步骤 2：编写 view 测试**

覆盖：

- 渲染仪表盘指标。
- 渲染用户列表。
- 点击用户行显示详情。
- 重置密码弹窗显示分段控件。
- 禁用用户在表格和右侧详情均为红色 UI。

- [ ] **步骤 3：实现页面**

视觉以 `docs/prototypes/admin-account-management.html` 为准：

- 顶部使用 `AppLayout accountType="ADMIN" activeRoute="admin"`。
- 指标卡 5 个。
- 用户表格。
- 右侧详情。
- 重置密码弹窗。
- 禁用/启用按钮。

- [ ] **步骤 4：运行测试**

```powershell
cd interval-client
npm run test -- adminService AdminAccountView
```

---

## 4. 集成验证

### 任务 11：全量测试和构建

- [ ] **步骤 1：后端全量测试**

```powershell
cd interval-server
./gradlew test
```

预期：所有测试通过。

- [ ] **步骤 2：前端全量测试**

```powershell
cd interval-client
npm run test
```

预期：所有测试通过。

- [ ] **步骤 3：前端构建**

```powershell
cd interval-client
npm run build
```

预期：构建成功。

### 任务 12：手工验收

- [ ] **步骤 1：启动后端和前端**

```powershell
cd interval-server
./gradlew bootRun
```

另一个终端：

```powershell
cd interval-client
npm run dev
```

- [ ] **步骤 2：默认管理员验收**

1. 访问前端登录页。
2. 使用 `admin / 123456` 登录。
3. 确认进入修改密码页。
4. 修改密码后确认进入 `/admin`。
5. 确认管理员顶部只显示“管理”。

- [ ] **步骤 3：普通用户验收**

1. 注册普通用户。
2. 登录后进入 `/time-grid`。
3. 确认普通用户顶部显示“时间格 / 统计”。
4. 访问 `/admin` 时不能进入管理页。

- [ ] **步骤 4：账号管理验收**

1. 管理员查看用户列表。
2. 重置普通用户密码。
3. 使用临时密码登录普通用户。
4. 确认普通用户被要求修改密码。
5. 管理员禁用普通用户。
6. 确认该普通用户不能登录。
7. 管理员启用该普通用户。
8. 确认该普通用户可以登录。

---

## 5. 风险和注意事项

- 当前数据库没有迁移工具。H2 开发环境可以依赖 JPA 自动更新；MySQL 环境需要补 SQL 迁移脚本或手工 ALTER 说明。
- 现有 `SeedDataInitializer` 已经创建了 admin 和分类，修改时要避免破坏已有用户数据。
- 不能把临时密码明文写入审计日志、控制台日志或数据库字段。
- 管理员账号不能进入时间格和统计，否则会产生无意义的分类和时间记录数据。
- 禁用账号的旧 JWT 必须被拦截，否则禁用只阻止登录、不阻止已登录访问。
- `lastActiveAt` 写入要节流，避免统计查询或频繁操作导致数据库压力。

## 6. 完成后建议

实现完成并测试通过后，使用 `requesting-code-review` 技能做一次代码审查，再决定是否合并。合并前需要确认：

- 原型与正式页面交互一致。
- 需求文档、实现计划和代码实现没有冲突。
- 默认管理员密码策略是否要在生产环境改为配置项。
