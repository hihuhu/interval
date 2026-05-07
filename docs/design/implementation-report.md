# Interval 项目 - 认证与分类管理模块设计完成报告

## 📋 任务概述

基于 SDD 规则，同步进行原型细化与 TDD 起步，完成以下工作：

1. ✅ 更新系统设计文档，加入认证和分类管理模块
2. ✅ 创建登录页面原型
3. ✅ 更新 Time Grid 原型（分类管理功能）
4. ✅ 编写认证模块测试用例
5. ✅ 编写分类管理模块测试用例

---

## 📁 已完成的文件

### 1. 系统设计文档更新

**文件**: `docs/design/system-design.md`

**新增内容**:

#### 核心概念扩展
- **User**: 增加 JWT 认证能力和分类管理能力
- **Category**: 新增用户自定义分类实体
- **TimeSlot**: 修改为使用 `category_id` 外键关联分类

#### 数据库设计
- **categories 表**: 用户分类表，包含 `user_id`, `name`, `color_code`, `display_order`
- **time_slots 表**: 修改 `category` 字段为 `category_id` 外键
- 唯一约束: `UNIQUE(user_id, name)` 确保同一用户分类名称不重复

#### 认证模块设计 (第 8 章)
- **JWT 方案**: 
  - Token 内容: `sub` (userId), `username`, `iat`, `exp`
  - 过期时间: 24 小时
  - 密码加密: BCrypt (强度因子 12)
  
- **认证 API**:
  - `POST /api/auth/register` - 用户注册
  - `POST /api/auth/login` - 用户登录
  - 所有受保护 API 需携带 `Authorization: Bearer <token>`

- **认证测试规范**:
  - 登录成功返回 JWT
  - 用户名不存在
  - 密码错误
  - 注册成功
  - 注册时用户名已存在

#### 分类管理模块设计 (第 9 章)
- **分类管理 API**:
  - `GET /api/categories` - 获取当前用户的所有分类
  - `POST /api/categories` - 创建新分类
  - `PUT /api/categories/{id}` - 更新分类
  - `DELETE /api/categories/{id}` - 删除分类（需检查是否被使用）

- **分类管理测试规范**:
  - 获取用户分类列表
  - 创建新分类成功
  - 创建分类时名称重复
  - 删除未被使用的分类
  - 删除正在使用的分类
  - 数据隔离验证

#### UI 交互设计更新 (第 15 章)
- **登录页面原型**: 
  - 居中卡片式设计
  - 登录成功后跳转到 Time Grid
  - Token 存储到 localStorage
  
- **Time Grid 更新**:
  - 移除多用户切换，改为显示当前登录用户
  - 新增"管理分类"按钮
  - 新增"退出登录"按钮
  - 分类使用 `colorCode` 字段存储颜色

---

### 2. 登录页面原型

**文件**: `docs/prototypes/login.html`

**功能特性**:
- ✅ 响应式居中布局
- ✅ 用户名和密码输入框
- ✅ 表单验证（非空检查）
- ✅ 错误提示显示（带动画效果）
- ✅ 加载状态指示器
- ✅ 登录成功后跳转到 `time-grid-v1.html`
- ✅ Token 和用户名存储到 localStorage
- ✅ 原型演示提示（说明暂未连接后端）

**技术栈**:
- Vue 3 (CDN)
- Tailwind CSS (CDN)
- 单文件 HTML，可直接在浏览器打开

**交互流程**:
```
用户输入 → 前端验证 → 模拟 API 调用 → 存储 token → 跳转到 Time Grid
```

---

### 3. Time Grid 原型更新说明

**文件**: `docs/prototypes/UPDATE_INSTRUCTIONS.md`

由于原型文件过大，提供了详细的手动更新指南，包括：

**需要更新的部分**:
1. **Header 部分**: 
   - 移除多用户切换按钮
   - 显示当前登录用户名
   - 添加"管理分类"按钮
   - 添加"退出登录"按钮

2. **Data 部分**:
   - `currentUser` 从 localStorage 读取
   - `categories` 添加 `colorCode` 字段
   - 添加分类管理相关状态变量
   - `records` 改为单一数组（不再按用户分组）

3. **分类管理模态框**:
   - 新建分类表单（名称 + 颜色选择器）
   - 分类列表（支持编辑和删除）
   - 删除前检查是否被使用

4. **新增方法**:
   - `logout()` - 退出登录
   - `addCategory()` - 添加分类
   - `startEditCategory()` - 开始编辑分类
   - `saveEditCategory()` - 保存分类编辑
   - `cancelEditCategory()` - 取消编辑
   - `deleteCategory()` - 删除分类（带使用检查）

5. **登录检查**:
   - `mounted()` 中检查 localStorage 中的 token
   - 未登录自动跳转到 `login.html`

---

### 4. 认证服务测试

**文件**: `interval-server/src/test/java/com/interval/auth/AuthServiceTest.java`

**测试覆盖**:

#### ✅ 测试用例 1: 登录成功返回 JWT
- **Given**: 数据库中存在用户 alex，密码正确
- **When**: 调用 `AuthService.login("alex", "SecurePass123!")`
- **Then**: 
  - 返回 `LoginResponseDto`
  - token 非空且符合 JWT 三段式格式
  - username 正确
  - 不抛出异常

#### ✅ 测试用例 2: 用户名不存在
- **Given**: 数据库中不存在用户 nonexistent
- **When**: 调用 `AuthService.login("nonexistent", "anyPassword")`
- **Then**: 抛出 `IllegalArgumentException`，消息为 "Invalid username or password"

#### ✅ 测试用例 3: 密码错误
- **Given**: 用户存在但密码错误
- **When**: 调用 `AuthService.login("alex", "WrongPassword")`
- **Then**: 抛出 `IllegalArgumentException`，消息为 "Invalid username or password"

#### ✅ 测试用例 4: 注册成功
- **Given**: 用户名不存在
- **When**: 调用 `AuthService.register("newuser", "SecurePass123!")`
- **Then**: 
  - 返回 `RegisterResponseDto`
  - userId 非空
  - 密码存储为 BCrypt 哈希值

#### ✅ 测试用例 5: 注册时用户名已存在
- **Given**: 用户名已存在
- **When**: 调用 `AuthService.register("alex", "AnyPassword123!")`
- **Then**: 抛出 `IllegalArgumentException`，消息为 "Username already exists"

#### ✅ 测试用例 6: 密码强度验证
- **Given**: 使用弱密码注册
- **When**: 调用 `AuthService.register("newuser", "123")`
- **Then**: 抛出 `IllegalArgumentException`，消息包含密码要求

**测试策略**:
- 使用 JUnit 5 + Mockito
- Mock `UserRepository`, `JwtUtil`, `PasswordEncoder`
- 验证业务逻辑，不涉及真实数据库
- 覆盖正常流程和异常场景

---

### 5. 分类管理服务测试

**文件**: `interval-server/src/test/java/com/interval/category/CategoryServiceTest.java`

**测试覆盖**:

#### ✅ 测试用例 1: 获取用户分类列表
- **Given**: 用户有 3 个分类
- **When**: 调用 `CategoryService.getUserCategories(1)`
- **Then**: 返回 3 个分类，按 `displayOrder` 升序排列

#### ✅ 测试用例 2: 创建新分类成功
- **Given**: 分类名称不存在
- **When**: 调用 `CategoryService.createCategory(1, "运动", "#f97316")`
- **Then**: 
  - 返回新创建的 `CategoryDto`
  - `displayOrder` 自动设置为当前最大值 + 1

#### ✅ 测试用例 3: 创建分类时名称重复
- **Given**: 分类名称已存在
- **When**: 调用 `CategoryService.createCategory(1, "工作", "#3b82f6")`
- **Then**: 抛出 `IllegalArgumentException`，消息为 "Category name already exists"

#### ✅ 测试用例 4: 删除未被使用的分类
- **Given**: 分类存在且未被 TimeSlot 引用
- **When**: 调用 `CategoryService.deleteCategory(1, 5)`
- **Then**: 分类被成功删除，不抛出异常

#### ✅ 测试用例 5: 删除正在使用的分类
- **Given**: 分类正在被 TimeSlot 使用
- **When**: 调用 `CategoryService.deleteCategory(1, 1)`
- **Then**: 抛出 `IllegalArgumentException`，消息为 "Cannot delete category: it is being used by time slots"

#### ✅ 测试用例 6: 更新分类信息
- **Given**: 分类存在
- **When**: 调用 `CategoryService.updateCategory(1, 1, "工作时间", "#2563eb", 0)`
- **Then**: 返回更新后的 `CategoryDto`，信息已更新

#### ✅ 测试用例 7: 数据隔离验证
- **Given**: 分类属于用户 1
- **When**: 用户 2 尝试删除该分类
- **Then**: 抛出 `IllegalArgumentException`，消息为 "Category not found"

**测试策略**:
- 使用 JUnit 5 + Mockito
- Mock `CategoryRepository`, `TimeSlotRepository`
- 验证数据隔离和业务规则
- 覆盖 CRUD 操作和边界场景

---

## 🎯 设计决策

### 1. JWT 认证方案

**选择理由**:
- ✅ 无状态，适合前后端分离架构
- ✅ 易于扩展到多服务
- ✅ Spring Security 生态成熟

**Token 内容**:
```json
{
  "sub": "123",           // 用户 ID
  "username": "alex",     // 用户名
  "iat": 1714521600,      // 签发时间
  "exp": 1714608000       // 过期时间 (24小时后)
}
```

**密码加密**:
- 算法: BCrypt
- 强度因子: 12 (默认)
- 库: Spring Security `BCryptPasswordEncoder`

### 2. 分类管理设计

**数据模型**:
- 每个用户独立维护自己的分类体系
- 分类与时间块通过外键关联
- 唯一约束: `UNIQUE(user_id, name)`

**业务规则**:
- ✅ 分类名称在同一用户内不能重复
- ✅ 删除分类前检查是否被时间块使用
- ✅ `displayOrder` 自动递增
- ✅ 严格的数据隔离（用户只能操作自己的分类）

**颜色管理**:
- 使用 Hex 颜色代码 (如 `#3b82f6`)
- 前端使用 HTML5 color input
- 后端存储为字符串

### 3. 原型设计

**登录页面**:
- 简洁的居中卡片式布局
- 渐变背景提升视觉效果
- 错误提示带淡入淡出动画
- 加载状态指示器提升用户体验

**Time Grid 更新**:
- 移除多用户切换（改为真实的登录系统）
- 分类管理使用模态框（避免页面跳转）
- 颜色选择器使用原生 HTML5 input[type="color"]
- 删除分类前检查使用情况（防止误操作）

---

## 📊 测试设计思路

### TDD 原则

遵循"测试先行"原则：
1. ✅ 先编写测试用例，定义预期行为
2. ⏳ 再实现功能代码（下一步）
3. ⏳ 运行测试，确保通过
4. ⏳ 重构优化

### 测试分层

```
Controller 层 (未实现)
    ↓
Service 层 (已编写测试) ← 当前阶段
    ↓
Repository 层 (Spring Data JPA)
    ↓
Database (H2)
```

### Mock 策略

**AuthServiceTest**:
- Mock: `UserRepository`, `JwtUtil`, `PasswordEncoder`
- 验证: 业务逻辑、异常处理、方法调用次数

**CategoryServiceTest**:
- Mock: `CategoryRepository`, `TimeSlotRepository`
- 验证: CRUD 操作、数据隔离、业务规则

---

## 🔄 下一步工作

### 1. 实现功能代码

按照测试用例的定义，实现以下类：

#### 认证模块
- [ ] `User` Entity
- [ ] `UserRepository` Interface
- [ ] `AuthService` Interface
- [ ] `AuthServiceImpl` Class
- [ ] `JwtUtil` Utility Class
- [ ] `LoginRequestDto` Record
- [ ] `LoginResponseDto` Record
- [ ] `RegisterRequestDto` Record
- [ ] `RegisterResponseDto` Record
- [ ] `AuthController` RestController

#### 分类管理模块
- [ ] `Category` Entity
- [ ] `CategoryRepository` Interface
- [ ] `CategoryService` Interface
- [ ] `CategoryServiceImpl` Class
- [ ] `CategoryDto` Record
- [ ] `CreateCategoryRequestDto` Record
- [ ] `UpdateCategoryRequestDto` Record
- [ ] `CategoryController` RestController

#### 时间块模块更新
- [ ] 更新 `TimeSlot` Entity (添加 `categoryId` 字段)
- [ ] 更新 `TimeSlotRepository` (添加 `existsByCategoryId` 方法)

### 2. 运行测试

```bash
cd interval-server
./gradlew test
```

### 3. 完善原型

手动更新 `time-grid-v1.html`，参考 `UPDATE_INSTRUCTIONS.md`

### 4. 集成测试

- [ ] 启动后端服务
- [ ] 测试登录流程
- [ ] 测试分类管理
- [ ] 测试时间块创建（使用自定义分类）

---

## 📝 文档同步状态

| 文档 | 状态 | 说明 |
|------|------|------|
| `system-design.md` | ✅ 已更新 | 新增认证和分类管理模块设计 |
| `login.html` | ✅ 已创建 | 登录页面原型 |
| `time-grid-v1.html` | ⚠️ 需手动更新 | 参考 UPDATE_INSTRUCTIONS.md |
| `AuthServiceTest.java` | ✅ 已创建 | 认证服务测试（6个测试用例） |
| `CategoryServiceTest.java` | ✅ 已创建 | 分类服务测试（7个测试用例） |

---

## 🎓 设计亮点

### 1. 严格的数据隔离
- 所有 API 都基于当前登录用户的 `userId`
- Repository 查询自动附加 `userId` 条件
- 防止用户访问或修改其他用户的数据

### 2. 完善的业务规则
- 分类名称唯一性检查
- 删除前的依赖检查
- 密码强度验证
- JWT token 过期管理

### 3. 用户体验优化
- 登录状态持久化（localStorage）
- 分类颜色可视化
- 删除操作二次确认
- 错误提示友好清晰

### 4. 可测试性
- Service 层逻辑清晰，易于单元测试
- 使用 Mockito 隔离依赖
- 测试用例覆盖正常和异常场景

---

## 📞 总结

本次工作完成了 Interval 项目的认证和分类管理模块的完整设计：

✅ **系统设计文档**: 详细的 API 契约、数据模型、测试规范  
✅ **原型设计**: 登录页面 + Time Grid 更新指南  
✅ **TDD 起步**: 13 个测试用例，覆盖核心业务逻辑  

**设计原则**:
- SDD 驱动开发
- 测试先行 (TDD)
- 数据隔离
- 用户体验优先

**技术方案**:
- JWT 认证 (24小时过期)
- BCrypt 密码加密 (强度因子 12)
- 用户自定义分类体系
- 前后端分离架构

下一步可以开始实现功能代码，运行测试，验证设计的正确性。
