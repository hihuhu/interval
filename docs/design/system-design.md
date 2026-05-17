# Interval 系统设计文档

## 1. 项目概述

Interval 是一个基于“柳比歇夫时间记录法”的时间块记录工具，用于帮助用户以固定时间粒度记录、回顾和分析每日活动。

系统采用“格子优先”的设计思想：一天被固定切分为 96 个连续的 15 分钟时间格，每个时间格称为一个 `TimeSlot`。用户记录时间时，不支持分钟级偏移，也不处理复杂的时间区间重叠问题，而是直接以格子为最小存储和操作单元。

核心目标：

- 支持多用户注册与登录。
- 每个用户独立维护自己的时间记录。
- 每天固定包含 96 个 15 分钟格子。
- 每个格子最多只能保存一条活动记录。
- 新记录保存到已有格子时，直接覆盖旧数据。
- 保证同一用户、同一天、同一格子在数据库中唯一。

## 2. 核心概念

### 2.1 User

`User` 表示系统用户。

用户能力：

- 注册账号。
- 登录系统（获取 JWT token）。
- 查看自己的时间格数据。
- 保存或覆盖自己的时间格数据。
- 管理自己的活动分类（新建、编辑、删除）。

系统中的所有 `TimeSlot` 和 `Category` 数据都必须归属于某一个 `User`。

### 2.2 Category

`Category` 表示用户自定义的活动分类。

每个用户可以创建自己的分类体系，用于标记时间块的类型。

字段要求：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 主键 |
| `userId` | Long | 所属用户 ID |
| `name` | String | 分类名称（如"工作"、"学习"） |
| `colorCode` | String | 颜色代码（如"#3b82f6"） |
| `status` | Enum | 分类状态：ACTIVE（激活）/ ARCHIVED（已归档） |
| `displayOrder` | Integer | 显示顺序 |
| `createdAt` | Instant | 创建时间 |
| `updatedAt` | Instant | 更新时间 |

#### 2.2.1 分类状态生命周期

分类具有以下状态：

- **ACTIVE（激活）**：正常可用，出现在"新增时间块"的分类下拉列表中
- **ARCHIVED（已归档）**：不可选择，但历史记录中仍可见并标注"(已归档)"

状态转换规则：

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: 创建分类
    ACTIVE --> ARCHIVED: 智能删除（有历史记录）
    ACTIVE --> [*]: 物理删除（无历史记录）
    ARCHIVED --> ACTIVE: 恢复分类（可选功能）
```

#### 2.2.2 智能删除逻辑

当用户删除分类时，系统自动判断并执行最安全的操作：

**场景 A：无历史记录**
- 检查：该分类从未在任何 TimeSlot 中使用
- 操作：物理删除（从数据库移除）
- 响应：`ApiResponse.data = { "action": "DELETED", "affectedRecords": 0 }`

**场景 B：有历史记录**
- 检查：存在 TimeSlot 引用该分类
- 操作：归档（UPDATE status = 'ARCHIVED'）
- 响应：`ApiResponse.data = { "action": "ARCHIVED", "affectedRecords": N }`
- 效果：
  - 分类在"新增时间块"下拉列表中消失
  - 历史记录回显时显示"工作 (已归档)"
  - 颜色保持不变

约束：

- 同一用户的活跃分类名称不能重复（允许 ACTIVE 和 ARCHIVED 同名共存）
- 每个用户注册时自动创建 5 个默认分类
- 删除分类时，系统自动检查并选择安全操作

### 2.3 TimeSlot

`TimeSlot` 是系统的核心存储单元，表示某个用户在某一天某个 15 分钟格子上的活动记录。

字段要求：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 主键 |
| `userId` | Long | 所属用户 ID |
| `date` | LocalDate | 日期 |
| `slotIndex` | Integer | 当天第几个 15 分钟格，范围 `0-95` |
| `activityName` | String | 活动名称 |
| `categoryId` | Long | 关联的分类 ID（外键） |
| `createdAt` | Instant | 创建时间 |
| `updatedAt` | Instant | 更新时间 |

#### 2.3.1 分类关联与回显策略

TimeSlot 通过 `categoryId` 外键关联 Category 表。查询历史记录时：

- 使用 LEFT JOIN 实时获取分类信息（名称、颜色、状态）
- 如果分类状态为 ARCHIVED，前端自动拼接"(已归档)"标注
- 如果分类已被物理删除（极少见），显示"未知分类"

**查询示例：**

```sql
SELECT 
    ts.id, ts.slot_index, ts.activity_name,
    c.id AS category_id,
    c.name AS category_name,
    c.color_code AS category_color,
    c.status AS category_status
FROM time_slots ts
LEFT JOIN categories c ON ts.category_id = c.id
WHERE ts.user_id = ? AND ts.date = ?
ORDER BY ts.slot_index
```

**DTO 结构：**

```typescript
{
  slotIndex: 36,
  activityName: "写代码",
  categoryId: 1,
  categoryName: "工作",
  categoryColor: "#3b82f6",
  categoryStatus: "ARCHIVED",
  displayName: "工作 (已归档)"  // 前端自动计算
}
```

`slotIndex` 与时间的换算关系：

| slotIndex | 时间范围 |
| --- | --- |
| `0` | `00:00 - 00:15` |
| `1` | `00:15 - 00:30` |
| `2` | `00:30 - 00:45` |
| `3` | `00:45 - 01:00` |
| `...` | `...` |
| `95` | `23:45 - 24:00` |

计算规则：

- 开始分钟数：`slotIndex * 15`
- 小时：`floor(slotIndex * 15 / 60)`
- 分钟：`slotIndex * 15 % 60`

## 3. 架构图预览

```mermaid
flowchart TD
    U[User] --> FE[Vue 3 Frontend]

    FE -->|GET /api/slots?date=yyyy-MM-dd| API[Spring Boot 3 REST API]
    FE -->|POST /api/slots| API

    API --> Auth[Authentication / User Context]
    API --> SlotService[TimeSlot Service]

    SlotService --> Validator[Slot Validation]
    SlotService --> Repo[JPA Repository]

    Repo --> DB[(H2 Database)]

    Validator --> Rule1[15-minute Grid Only]
    Validator --> Rule2[slotIndex 0-95]
    Validator --> Rule3[userId + date + slotIndex Unique]
    Validator --> Rule4[Save Means Upsert / Overwrite]
```

## 4. 技术栈规划

### 4.1 后端

后端采用 Java 17 与 Spring Boot 3。

主要技术：

| 技术 | 用途 |
| --- | --- |
| Java 17 | 后端开发语言 |
| Spring Boot 3 | 应用框架 |
| Spring Web | REST API |
| Spring Data JPA | 数据访问层 |
| H2 Database | 开发与本地运行数据库 |
| Bean Validation | 请求参数校验 |

后端职责：

- 用户注册与登录。
- 根据当前登录用户隔离数据。
- 提供时间格查询接口。
- 提供批量保存/覆盖接口。
- 保证 `userId + date + slotIndex` 唯一。
- 执行 `slotIndex` 范围校验。
- 执行覆盖逻辑。

### 4.2 前端

前端采用 Vue 3 与 Tailwind CSS。

主要技术：

| 技术 | 用途 |
| --- | --- |
| Vue 3 | 前端应用框架 |
| TypeScript | 类型安全 |
| Tailwind CSS | UI 样式与 96 个时间格绘制 |
| Fetch / Axios | 调用后端 API |

前端职责：

- 展示每日 96 个时间格。
- 根据 `slotIndex` 绘制固定 15 分钟刻度。
- 支持用户选择一个或多个格子。
- 支持填写活动名称与分类。
- 调用后端批量保存接口。
- 从后端获取某日 96 格数据并渲染。

## 5. 核心业务规则

### 5.1 格子优先

系统只支持 15 分钟粒度的时间记录。

约束：

- 不支持任意开始时间。
- 不支持分钟级偏移。
- 不保存 `startTime` 与 `endTime` 作为核心字段。
- 时间位置由 `date + slotIndex` 唯一确定。
- 每天固定有 96 个格子。

合法示例：

```json
{
  "date": "2026-04-30",
  "slotIndex": 36,
  "activityName": "阅读",
  "category": "学习"
}
```

非法示例：

```json
{
  "date": "2026-04-30",
  "startTime": "09:07",
  "endTime": "09:42",
  "activityName": "阅读",
  "category": "学习"
}
```

### 5.2 覆盖逻辑

格子具有独占性。

当用户保存某个 `date + slotIndex` 的记录时：

- 如果该格子不存在记录，则新增。
- 如果该格子已存在记录，则直接覆盖旧的 `activityName` 与 `category`。
- 不计算重叠。
- 不合并时间段。
- 不设置优先级。
- 不保留被覆盖记录的冲突状态。

覆盖判断范围必须包含当前用户，即：

```text
userId + date + slotIndex
```

### 5.3 数据完整性

数据库必须保证以下唯一约束：

```text
UNIQUE(user_id, date, slot_index)
```

该约束用于确保：

- 同一用户同一天同一格子最多只有一条记录。
- 不同用户可以在同一天同一格子记录不同活动。
- 同一用户不同日期的同一格子互不影响。

## 6. 数据库 Schema 设计

### 6.1 users 表

```sql
CREATE TABLE users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

字段说明：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | 用户主键 |
| `username` | VARCHAR(100) | NOT NULL, UNIQUE | 登录用户名 |
| `password_hash` | VARCHAR(255) | NOT NULL | 密码哈希 |
| `created_at` | TIMESTAMP | NOT NULL | 创建时间 |
| `updated_at` | TIMESTAMP | NOT NULL | 更新时间 |

### 6.2 categories 表

```sql
CREATE TABLE categories (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    color_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT ck_categories_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);
```

字段说明：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | 分类主键 |
| `user_id` | BIGINT | FK, NOT NULL | 所属用户 |
| `name` | VARCHAR(100) | NOT NULL | 分类名称 |
| `color_code` | VARCHAR(20) | NOT NULL | 颜色代码（如 #3b82f6） |
| `status` | VARCHAR(20) | NOT NULL | 分类状态：ACTIVE / ARCHIVED |
| `display_order` | INTEGER | NOT NULL | 显示顺序 |
| `created_at` | TIMESTAMP | NOT NULL | 创建时间 |
| `updated_at` | TIMESTAMP | NOT NULL | 更新时间 |

推荐索引：

```sql
CREATE INDEX idx_categories_user_status
ON categories(user_id, status);
```

**唯一约束说明：**

为支持"归档后重新创建同名分类"，不设置数据库级唯一约束。业务层通过以下查询保证活跃分类名称唯一：

```sql
SELECT COUNT(*) FROM categories 
WHERE user_id = ? AND name = ? AND status = 'ACTIVE'
```

### 6.3 time_slots 表

```sql
CREATE TABLE time_slots (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    slot_index INTEGER NOT NULL,
    activity_name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_time_slots_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_time_slots_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_time_slots_user_date_slot
        UNIQUE (user_id, date, slot_index),

    CONSTRAINT ck_time_slots_slot_index
        CHECK (slot_index >= 0 AND slot_index <= 95)
);
```

字段说明：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | 时间格主键 |
| `user_id` | BIGINT | FK, NOT NULL | 所属用户 |
| `date` | DATE | NOT NULL | 日期 |
| `slot_index` | INTEGER | NOT NULL, `0-95` | 当天第几个 15 分钟格 |
| `activity_name` | VARCHAR(255) | NOT NULL | 活动名称 |
| `category_id` | BIGINT | FK, NOT NULL | 所属分类 ID |
| `created_at` | TIMESTAMP | NOT NULL | 创建时间 |
| `updated_at` | TIMESTAMP | NOT NULL | 更新时间 |

推荐索引：

```sql
CREATE INDEX idx_time_slots_user_date
ON time_slots(user_id, date);

CREATE INDEX idx_time_slots_category
ON time_slots(category_id);
```

**外键约束说明：**

- `ON DELETE RESTRICT`：防止误删除有历史记录的分类
- 智能删除逻辑会先检查 `COUNT(*)`，有记录则归档而非删除
- 归档的分类保留在数据库中，外键关系不受影响

## 7. 后端领域模型设计

### 7.1 User Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

### 7.2 Category Entity

```java
@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "color_code", nullable = false, length = 20)
    private String colorCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

### 7.2.1 CategoryStatus Enum

```java
public enum CategoryStatus {
    ACTIVE,    // 激活状态，可在新增时间块时选择
    ARCHIVED   // 已归档，不可选择，但历史记录可见
}
```

### 7.3 TimeSlot Entity

```java
@Entity
@Table(
    name = "time_slots",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_time_slots_user_date_slot",
            columnNames = {"user_id", "date", "slot_index"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @Column(name = "activity_name", nullable = false)
    private String activityName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

**关联说明：**

- 使用 `@ManyToOne` 关联 Category 实体
- `FetchType.LAZY` 延迟加载，避免不必要的查询
- 查询历史记录时使用 `@EntityGraph` 避免 N+1 问题

## 8. 认证模块设计

### 8.1 认证方案

系统采用 JWT (JSON Web Token) 进行无状态认证。

#### 8.1.1 JWT 内容设计

```json
{
  "sub": "123",
  "username": "alex",
  "iat": 1714521600,
  "exp": 1714608000
}
```

| 字段 | 说明 |
| --- | --- |
| `sub` | 用户 ID（主键） |
| `username` | 用户名 |
| `iat` | 签发时间（Issued At） |
| `exp` | 过期时间（Expiration Time） |

#### 8.1.2 Token 过期时间

- 默认过期时间：**24 小时**
- 可通过配置文件调整：`app.jwt.expiration-hours=24`

#### 8.1.3 密码加密

- 使用 **BCrypt** 算法
- Spring Security 提供的 `BCryptPasswordEncoder`
- 强度因子：12（默认）

#### 8.1.4 认证流程

```mermaid
sequenceDiagram
    participant C as Client
    participant A as AuthController
    participant S as AuthService
    participant R as UserRepository
    participant J as JwtUtil

    C->>A: POST /api/auth/login
    A->>S: login(username, password)
    S->>R: findByUsername(username)
    R-->>S: User entity
    S->>S: BCrypt.matches(password, hash)
    S->>J: generateToken(userId, username)
    J-->>S: JWT token
    S-->>A: LoginResponseDto(token, username)
    A-->>C: 200 OK + token
    
    Note over C: Store token in localStorage
    
    C->>A: GET /api/slots (with Authorization header)
    A->>A: Extract & validate JWT
    A->>A: Get userId from token
    A->>S: getSlots(userId, date)
```

### 8.2 认证 API 契约

#### 8.2.1 用户注册

```http
POST /api/auth/register
```

**Request:**

```json
{
  "username": "alex",
  "password": "SecurePass123!"
}
```

**Response:**

```json
{
  "result": "SUCCESS",
  "message": "User registered successfully",
  "data": {
    "userId": 123,
    "username": "alex"
  }
}
```

**错误响应：**

用户名已存在：

```json
{
  "result": "ERROR",
  "message": "Username already exists",
  "data": null
}
```

密码强度不足：

```json
{
  "result": "ERROR",
  "message": "Password must be at least 8 characters",
  "data": null
}
```

#### 8.2.2 用户登录

```http
POST /api/auth/login
```

**Request:**

```json
{
  "username": "alex",
  "password": "SecurePass123!"
}
```

**Response:**

```json
{
  "result": "SUCCESS",
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "alex",
    "expiresIn": 86400
  }
}
```

**错误响应：**

用户名不存在：

```json
{
  "result": "ERROR",
  "message": "Invalid username or password",
  "data": null
}
```

密码错误：

```json
{
  "result": "ERROR",
  "message": "Invalid username or password",
  "data": null
}
```

#### 8.2.3 Token 验证

所有需要认证的 API 请求必须在 HTTP Header 中携带 token：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

后端通过 Filter 或 Interceptor 自动验证 token 并提取 `userId`。

### 8.3 认证测试规范

#### 测试用例 1：登录成功返回 JWT

**Given:**
- 数据库中存在用户 `alex`，密码哈希为 BCrypt 加密的 `SecurePass123!`

**When:**
- 调用 `AuthService.login("alex", "SecurePass123!")`

**Then:**
- 返回 `LoginResponseDto`
- `token` 字段非空
- `token` 格式符合 JWT 规范（三段式，用 `.` 分隔）
- `username` 字段为 `"alex"`
- 不抛出任何异常

#### 测试用例 2：用户名不存在

**Given:**
- 数据库中不存在用户 `nonexistent`

**When:**
- 调用 `AuthService.login("nonexistent", "anyPassword")`

**Then:**
- 抛出 `IllegalArgumentException`
- 异常消息为 `"Invalid username or password"`

#### 测试用例 3：密码错误

**Given:**
- 数据库中存在用户 `alex`，密码哈希为 BCrypt 加密的 `SecurePass123!`

**When:**
- 调用 `AuthService.login("alex", "WrongPassword")`

**Then:**
- 抛出 `IllegalArgumentException`
- 异常消息为 `"Invalid username or password"`

#### 测试用例 4：注册成功并初始化默认分类

**Given:**
- 数据库中不存在用户 `newuser`

**When:**
- 调用 `AuthService.register("newuser", "SecurePass123!")`

**Then:**
- 返回 `RegisterResponseDto`
- `userId` 字段非空
- `username` 字段为 `"newuser"`
- 数据库中新增一条用户记录
- 密码字段存储的是 BCrypt 哈希值，不是明文
- 自动创建 5 个默认分类：睡眠 🛌、工作 💻、学习 📚、运动 🏃、杂项 ☕

#### 测试用例 5：注册时用户名已存在

**Given:**
- 数据库中已存在用户 `alex`

**When:**
- 调用 `AuthService.register("alex", "AnyPassword123!")`

**Then:**
- 抛出 `IllegalArgumentException`
- 异常消息为 `"Username already exists"`

### 8.4 默认分类初始化

新用户注册时，系统自动创建以下默认分类：

| 分类名称 | 颜色代码 | 显示顺序 |
|---------|---------|---------|
| 睡眠 | #64748b | 0 |
| 工作 | #3b82f6 | 1 |
| 学习 | #8b5cf6 | 2 |
| 运动 | #f97316 | 3 |
| 杂项 | #6b7280 | 4 |

用户可以自由删除或修改这些默认分类。

## 9. 分类管理模块设计

### 9.1 分类管理需求

每个用户可以自定义自己的活动分类体系。

核心能力：

- 查看自己的活跃分类（status = ACTIVE）
- 新建分类（指定名称和颜色）
- 编辑分类（修改名称、颜色、显示顺序）
- 智能删除分类（自动判断物理删除或归档）
- 分类按 `display_order` 排序显示

### 9.2 分类 API 契约

#### 9.2.1 获取当前用户的活跃分类

```http
GET /api/categories
```

**说明：** 默认仅返回 `status = ACTIVE` 的分类，用于"新增时间块"的下拉列表。

**Response:**

```json
{
  "result": "SUCCESS",
  "message": "Categories retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "工作",
      "colorCode": "#3b82f6",
      "status": "ACTIVE",
      "displayOrder": 0
    },
    {
      "id": 2,
      "name": "学习",
      "colorCode": "#8b5cf6",
      "status": "ACTIVE",
      "displayOrder": 1
    }
  ]
}
```

#### 9.2.2 创建新分类

```http
POST /api/categories
```

**Request:**

```json
{
  "name": "运动",
  "colorCode": "#f97316"
}
```

**Response:**

```json
{
  "result": "SUCCESS",
  "message": "Category created successfully",
  "data": {
    "id": 3,
    "name": "运动",
    "colorCode": "#f97316",
    "status": "ACTIVE",
    "displayOrder": 2
  }
}
```

**错误响应：**

活跃分类名称已存在：

```json
{
  "result": "ERROR",
  "message": "Active category with this name already exists",
  "data": null
}
```

#### 9.2.3 更新分类

```http
PUT /api/categories/{id}
```

**Request:**

```json
{
  "name": "工作时间",
  "colorCode": "#2563eb",
  "displayOrder": 0
}
```

**Response:**

```json
{
  "result": "SUCCESS",
  "message": "Category updated successfully",
  "data": {
    "id": 1,
    "name": "工作时间",
    "colorCode": "#2563eb",
    "status": "ACTIVE",
    "displayOrder": 0
  }
}
```

#### 9.2.4 智能删除分类

```http
DELETE /api/categories/{id}
```

**场景 A：无历史记录（物理删除）**

```json
{
  "result": "SUCCESS",
  "message": "Category deleted successfully",
  "data": {
    "action": "DELETED",
    "affectedRecords": 0
  }
}
```

**场景 B：有历史记录（自动归档）**

```json
{
  "result": "SUCCESS",
  "message": "Category has 15 time records and has been archived",
  "data": {
    "action": "ARCHIVED",
    "affectedRecords": 15
  }
}
```

**前端处理建议：**

```javascript
if (response.data.action === 'ARCHIVED') {
  showNotification({
    type: 'info',
    title: '分类已归档',
    message: `检测到该分类已有 ${response.data.affectedRecords} 条历史记录，已自动为您转为归档状态。历史记录将保留并标注"(已归档)"。`
  });
}
```

### 9.3 分类管理测试规范

#### 测试用例 1：获取用户活跃分类列表

**Given:**
- 用户 `alex` (userId=1) 有 3 个 ACTIVE 分类和 1 个 ARCHIVED 分类

**When:**
- 调用 `CategoryService.getActiveCategories(1)`

**Then:**
- 返回包含 3 个 `CategoryDto` 的列表（不包含 ARCHIVED）
- 按 `displayOrder` 升序排列

#### 测试用例 2：创建新分类成功

**Given:**
- 用户 `alex` (userId=1) 没有名为"运动"的活跃分类

**When:**
- 调用 `CategoryService.createCategory(1, "运动", "#f97316")`

**Then:**
- 返回新创建的 `CategoryDto`
- `id` 字段非空
- `status` 为 `ACTIVE`
- `displayOrder` 自动设置为当前最大值 + 1

#### 测试用例 3：创建分类时活跃名称重复

**Given:**
- 用户 `alex` (userId=1) 已有名为"工作"的 ACTIVE 分类

**When:**
- 调用 `CategoryService.createCategory(1, "工作", "#3b82f6")`

**Then:**
- 抛出 `IllegalArgumentException`
- 异常消息为 `"Active category with this name already exists"`

#### 测试用例 4：删除无记录的分类（物理删除）

**Given:**
- 用户 `alex` (userId=1) 有分类 `id=5`
- 没有任何 `TimeSlot` 引用该分类

**When:**
- 调用 `CategoryService.deleteCategory(1, 5)`

**Then:**
- 返回 `DeleteCategoryResponseDto`
- `action` 为 `"DELETED"`
- `affectedRecords` 为 `0`
- 数据库中该分类已被物理删除

#### 测试用例 5：删除有记录的分类（自动归档）

**Given:**
- 用户 `alex` (userId=1) 有分类 `id=1`
- 存在 3 条 `TimeSlot` 记录的 `category_id=1`

**When:**
- 调用 `CategoryService.deleteCategory(1, 1)`

**Then:**
- 返回 `DeleteCategoryResponseDto`
- `action` 为 `"ARCHIVED"`
- `affectedRecords` 为 `3`
- 数据库中该分类 `status` 变为 `ARCHIVED`
- 分类未被物理删除

#### 测试用例 6：归档后不出现在活跃列表

**Given:**
- 用户 `alex` (userId=1) 有 3 个分类
- "学习"分类有历史记录并被归档

**When:**
- 调用 `CategoryService.getActiveCategories(1)`

**Then:**
- 返回列表不包含"学习"分类
- 只包含 ACTIVE 状态的分类

#### 测试用例 7：历史记录正确回显归档状态

**Given:**
- 用户创建分类"学习"并记录时间块
- 归档"学习"分类

**When:**
- 查询该日期的时间块

**Then:**
- 返回的 DTO 中 `categoryStatus` 为 `"ARCHIVED"`
- `displayName` 为 `"学习 (已归档)"`
- `categoryName` 为 `"学习"`
- `categoryColor` 保持不变

#### 测试用例 8：归档后可重新创建同名活跃分类

**Given:**
- 用户创建并归档"运动"分类

**When:**
- 用户重新创建同名分类"运动"（但颜色不同）

**Then:**
- 创建成功，且 ID 不同
- 新分类 `status` 为 `ACTIVE`
- 旧分类依然存在且 `status` 为 `ARCHIVED`
- 活跃分类列表中只显示新分类

## 10. 核心 API 契约

### 10.1 获取某日 96 格数据

```http
GET /api/slots?date=2026-04-30
```

#### 说明

获取当前登录用户在指定日期的时间格数据。

后端应返回完整的 96 个格子，而不仅仅是数据库中已有记录的格子。没有记录的格子以空状态返回，便于前端直接渲染 96 格 UI。

#### Query Parameters

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `date` | string | 是 | 日期，格式 `yyyy-MM-dd` |

#### Response

```json
{
  "date": "2026-04-30",
  "slots": [
    {
      "slotIndex": 0,
      "activityName": null,
      "category": null
    },
    {
      "slotIndex": 1,
      "activityName": "睡眠",
      "category": "休息"
    },
    {
      "slotIndex": 2,
      "activityName": "睡眠",
      "category": "休息"
    }
  ]
}
```

#### Response 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `date` | string | 查询日期 |
| `slots` | array | 固定长度为 96 的时间格数组 |
| `slots[].slotIndex` | number | 格子索引，范围 `0-95` |
| `slots[].activityName` | string/null | 活动名称 |
| `slots[].category` | string/null | 活动分类 |

#### 错误响应

日期格式错误：

```json
{
  "code": "INVALID_DATE",
  "message": "date must use yyyy-MM-dd format"
}
```

未登录：

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication is required"
}
```

### 10.2 批量保存/覆盖格子

```http
POST /api/slots
```

#### 说明

批量保存当前登录用户某一天的一个或多个时间格。

保存规则：

- `slotIndex` 必须在 `0-95` 范围内。
- 同一次请求中不应出现重复的 `slotIndex`。
- 如果目标格子已存在，则覆盖。
- 如果目标格子不存在，则新增。
- 覆盖仅影响当前登录用户自己的数据。

#### Request

```json
{
  "date": "2026-04-30",
  "slots": [
    {
      "slotIndex": 36,
      "activityName": "阅读《奇特的一生》",
      "category": "学习"
    },
    {
      "slotIndex": 37,
      "activityName": "阅读《奇特的一生》",
      "category": "学习"
    }
  ]
}
```

#### Request 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `date` | string | 是 | 日期，格式 `yyyy-MM-dd` |
| `slots` | array | 是 | 待保存的格子列表 |
| `slots[].slotIndex` | number | 是 | 格子索引，范围 `0-95` |
| `slots[].activityName` | string | 是 | 活动名称 |
| `slots[].category` | string | 是 | 活动分类 |

#### Response

```json
{
  "date": "2026-04-30",
  "savedCount": 2,
  "slots": [
    {
      "slotIndex": 36,
      "activityName": "阅读《奇特的一生》",
      "category": "学习"
    },
    {
      "slotIndex": 37,
      "activityName": "阅读《奇特的一生》",
      "category": "学习"
    }
  ]
}
```

#### 错误响应

`slotIndex` 越界：

```json
{
  "code": "INVALID_SLOT_INDEX",
  "message": "slotIndex must be between 0 and 95"
}
```

请求中存在重复格子：

```json
{
  "code": "DUPLICATE_SLOT_INDEX",
  "message": "slots must not contain duplicate slotIndex values"
}
```

日期格式错误：

```json
{
  "code": "INVALID_DATE",
  "message": "date must use yyyy-MM-dd format"
}
```

未登录：

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication is required"
}
```

## 11. 保存/覆盖算法

### 11.1 输入

```text
currentUserId
date
slots[]
```

### 11.2 处理流程

```mermaid
flowchart TD
    A[Receive POST /api/slots] --> B[Get current userId]
    B --> C[Validate date]
    C --> D[Validate slots not empty]
    D --> E[Validate slotIndex in 0-95]
    E --> F[Validate no duplicate slotIndex in request]
    F --> G[Load existing slots by userId + date + slotIndex IN request]
    G --> H{Slot exists?}
    H -->|Yes| I[Overwrite activityName and category]
    H -->|No| J[Create new TimeSlot]
    I --> K[Save all]
    J --> K
    K --> L[Return saved slots]
```

### 11.3 伪代码

```text
function saveSlots(userId, date, requestSlots):
    validate(date)
    validateEachSlotIndexBetween0And95(requestSlots)
    validateNoDuplicateSlotIndex(requestSlots)

    existingSlots = findByUserIdAndDateAndSlotIndexIn(
        userId,
        date,
        requestSlots.slotIndexes
    )

    existingByIndex = map existingSlots by slotIndex

    result = []

    for requestSlot in requestSlots:
        if existingByIndex contains requestSlot.slotIndex:
            slot = existingByIndex[requestSlot.slotIndex]
            slot.activityName = requestSlot.activityName
            slot.category = requestSlot.category
            slot.updatedAt = now
        else:
            slot = new TimeSlot(
                userId = userId,
                date = date,
                slotIndex = requestSlot.slotIndex,
                activityName = requestSlot.activityName,
                category = requestSlot.category,
                createdAt = now,
                updatedAt = now
            )

        result.add(slot)

    saveAll(result)

    return result
```

## 12. 前端 96 格设计

前端以 `GET /api/slots` 返回的 96 个格子作为渲染基础。

建议 UI 结构：

- 一天固定显示 96 个格子。
- 每个格子代表 15 分钟。
- 使用 Tailwind CSS Grid 绘制时间块。
- 可按小时分组，每小时 4 个格子。
- 空格子显示为空状态。
- 有记录的格子显示 `activityName` 与 `category`。
- 用户选择多个连续或非连续格子后，可统一填写活动名称与分类。
- 保存时调用 `POST /api/slots` 批量覆盖。

示例布局：

```html
<div class="grid grid-cols-4 gap-1">
  <button class="h-8 rounded border text-xs">
    00:00
  </button>
  <button class="h-8 rounded border text-xs">
    00:15
  </button>
  <button class="h-8 rounded border text-xs">
    00:30
  </button>
  <button class="h-8 rounded border text-xs">
    00:45
  </button>
</div>
```

## 13. 安全与数据隔离

### 13.1 全局权限控制

除登录/注册接口外，所有 API 必须进行 JWT 校验。

#### 13.1.1 JWT 拦截器

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 白名单：登录/注册接口不需要校验
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 提取 Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response, "Missing or invalid token");
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            // 验证并解析 token
            Long userId = jwtUtil.extractUserId(token);
            String username = jwtUtil.extractUsername(token);
            
            // 将用户信息存入请求上下文
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            
            filterChain.doFilter(request, response);
            
        } catch (JwtException e) {
            sendUnauthorizedResponse(response, "Invalid or expired token");
        }
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) 
        throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
            String.format("{\"result\":\"ERROR\",\"message\":\"%s\",\"data\":null}", message)
        );
    }
}
```

#### 13.1.2 Controller 层获取当前用户

```java
@RestController
@RequestMapping("/api/slots")
public class TimeSlotController {
    
    @Autowired
    private TimeSlotService timeSlotService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getSlots(
        @RequestParam String date,
        HttpServletRequest request
    ) {
        // 从请求上下文获取当前用户 ID（由拦截器注入）
        Long userId = (Long) request.getAttribute("userId");
        
        LocalDate localDate = LocalDate.parse(date);
        List<TimeSlotDto> slots = timeSlotService.getSlotsByDate(userId, localDate);
        
        return ResponseEntity.ok(ApiResponse.success("Slots retrieved", slots));
    }
}
```

### 13.2 数据隔离

系统必须通过当前登录态识别用户。

API 不应允许客户端直接传入 `userId` 来读写时间格数据。

正确方式：

- 后端从认证上下文中获取当前 `userId`。
- 查询时自动附加 `userId` 条件。
- 保存时自动写入当前 `userId`。
- 唯一约束使用 `user_id + date + slot_index`。

禁止行为：

- `GET /api/slots?userId=1&date=...`
- `POST /api/slots` 请求体中携带 `userId`
- 允许用户覆盖其他用户的格子

**数据隔离验证：**

确保用户 A 无法通过猜测 ID 来查询或修改用户 B 的分类或时间块：

```java
// 正确：Service 层强制校验 userId
public CategoryDto getCategory(Long userId, Long categoryId) {
    Category category = categoryRepository.findByUserIdAndId(userId, categoryId)
        .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    return toDto(category);
}

// 错误：仅通过 categoryId 查询，可能泄露其他用户数据
public CategoryDto getCategory(Long categoryId) {
    Category category = categoryRepository.findById(categoryId)
        .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    return toDto(category);
}
```

## 14. 非目标

当前阶段不处理以下能力：

- 分钟级时间记录。
- 跨天时间段记录。
- 时间段重叠计算。
- 活动优先级。
- 冲突解决策略。
- 历史版本回溯。
- 被覆盖记录恢复。
- 多时区复杂换算。

## 15. 设计总结

### 15.1 核心设计决策

Interval 的核心设计是将时间记录问题简化为固定 96 格的状态管理问题。

该设计带来的收益：

- 数据模型简单。
- 前后端契约清晰。
- UI 渲染稳定。
- 保存逻辑确定。
- 覆盖行为可预期。
- 数据库唯一约束能够直接保障核心一致性。

核心不变量：

```text
For each userId + date + slotIndex, there is at most one TimeSlot.
```

该不变量应同时由业务逻辑与数据库唯一约束共同保证。

### 15.2 智能删除与归档设计

分类管理采用"智能删除"策略，平衡了数据安全和用户体验：

**设计优势：**

1. **自动判断**：系统根据历史记录自动选择物理删除或归档
2. **数据安全**：有历史记录的分类不会被误删除
3. **历史完整**：归档后历史记录依然可见且带标注
4. **用户友好**：删除时自动提示受影响的记录数量
5. **灵活重建**：归档后可创建同名的新分类

**技术实现：**

- 使用 `status` 枚举管理分类状态（ACTIVE / ARCHIVED）
- TimeSlot 通过外键关联 Category，查询时 LEFT JOIN 获取实时状态
- 前端根据 `categoryStatus` 自动拼接"(已归档)"标注
- 性能可控：查询范围小（单日最多 96 条），JOIN 开销可接受

**状态转换：**

```text
创建 → ACTIVE → 删除（无记录）→ 物理删除
              → 删除（有记录）→ ARCHIVED → 可选：恢复 → ACTIVE
```

### 15.3 权限与数据隔离

- JWT 拦截器校验所有非认证接口
- Controller 从请求上下文获取 userId，不信任客户端传参
- Service 层强制校验 userId，防止跨用户数据访问
- 数据库外键约束 + 业务逻辑双重保障数据完整性

## 16. UI 交互设计

### 16.1 登录页面原型

#### 16.1.1 登录页面布局

登录页面采用居中卡片式设计，简洁清晰。

布局结构：

```text
┌─────────────────────────────────────┐
│                                     │
│         Interval                    │
│    时间记录从这里开始                  │
│                                     │
│    ┌─────────────────────┐         │
│    │  用户名              │         │
│    │  [ alex        ]    │         │
│    │                     │         │
│    │  密码                │         │
│    │  [ ********    ]    │         │
│    │                     │         │
│    │  [    登录    ]     │         │
│    │                     │         │
│    │  还没有账号？注册     │         │
│    └─────────────────────┘         │
│                                     │
└─────────────────────────────────────┘
```

#### 16.1.2 登录交互流程

```mermaid
sequenceDiagram
    participant U as User
    participant L as Login Page
    participant A as API
    participant S as Storage

    U->>L: 输入用户名和密码
    U->>L: 点击"登录"按钮
    L->>L: 前端验证（非空）
    L->>A: POST /api/auth/login
    A-->>L: 200 OK + JWT token
    L->>S: localStorage.setItem('token', token)
    L->>S: localStorage.setItem('username', username)
    L->>L: window.location.href = '/time-grid.html'
    
    Note over U: 跳转到 96 格子主页
```

#### 16.1.3 错误处理

登录失败时，在表单下方显示错误提示：

```text
❌ 用户名或密码错误，请重试
```

网络错误时：

```text
❌ 网络连接失败，请检查网络后重试
```

#### 16.1.4 原型文件

```text
docs/prototypes/login.html
```

### 16.2 Time Grid 原型目标

为了验证核心交互体验，我们在 `docs/prototypes/time-grid-v1.html` 中提供了一个高保真单文件原型。

该原型使用 Tailwind CSS + Vue 3 CDN 实现，可直接在浏览器中打开查看效果。

原型目标：

- 展示 24 行 x 4 列的时间网格（共 96 个格子）
- 支持点击和拖拽选择连续格子
- 支持为选中格子填写分类和备注内容
- 支持查看已记录格子的完整信息
- 支持编辑和删除已有记录
- 支持多用户切换模拟
- 使用颜色区分不同分类的时间块

### 16.3 时间网格布局：24 行 x 4 列

每一行代表 1 小时，每一列代表 15 分钟。

布局示意：

```text
00:00  □ □ □ □
01:00  □ □ □ □
02:00  □ □ □ □
...
22:00  □ □ □ □
23:00  □ □ □ □
```

每行左侧显示小时标签，例如 `09:00`。

每个格子代表一个 15 分钟时间段：

| 行 | 列 | slotIndex | 时间段 |
|---|---|---|---|
| 9 | 0 | 36 | 09:00 - 09:15 |
| 9 | 1 | 37 | 09:15 - 09:30 |
| 9 | 2 | 38 | 09:30 - 09:45 |
| 9 | 3 | 39 | 09:45 - 10:00 |

这种布局的优势：

- 时间感更直观，一行就是一小时
- 更适合记录日程和回顾一天
- 更容易多选连续时间段
- 视觉上接近日历/时间轴

### 16.4 核心交互模型：记录块（Record）

用户保存的不是孤立的格子，而是一条完整的时间记录块。

例如用户选择 `09:00 - 09:30` 并填写「看书」，系统保存的是：

```json
{
  "startIndex": 36,
  "endIndex": 38,
  "category": "学习",
  "note": "看书",
  "cells": [36, 37]
}
```

这条记录覆盖两个格子：

- `slotIndex 36`: 09:00 - 09:15
- `slotIndex 37`: 09:15 - 09:30

之后用户点击任意一个格子（例如 `09:15 - 09:30`），右侧都会显示完整记录：

```text
记录详情

当前格子
09:15 - 09:30

所属时间块
09:00 - 09:30

分类
学习

备注内容
看书
```

这个设计符合用户心智：**格子是可视化单位，记录块是语义单位**。

### 16.5 格子状态与视觉反馈

每个格子有三种状态：

| 状态 | 视觉 | 说明 |
|---|---|---|
| 空白 | 浅灰色背景 | 未记录任何活动 |
| 选中 | 高亮边框 | 用户正在选择 |
| 已记录 | 分类颜色 | 已保存记录，颜色由分类决定 |

分类颜色映射：

| 分类 | Tailwind 颜色 | 视觉效果 |
|---|---|---|
| 工作 | blue-500 | 蓝色 |
| 学习 | violet-500 | 紫色 |
| 休息 | green-500 | 绿色 |
| 运动 | orange-500 | 橙色 |
| 通勤 | amber-500 | 黄色 |
| 娱乐 | pink-500 | 粉色 |
| 其他 | slate-400 | 灰色 |

同一记录块的所有格子使用相同颜色。

当用户 hover 或点击某个格子时，同一记录块的所有格子会联动高亮，帮助用户识别时间块边界。

### 16.6 点击与拖拽交互

#### 点击空白格子

- 选中该格子
- 右侧显示「新建记录」表单
- 显示选中时间范围
- 用户填写分类和备注后保存

#### 拖拽多个空白格子

- 按下鼠标并拖动，选中连续多个格子
- 自动计算起止时间和持续时长
- 右侧显示「新建记录」表单
- 用户填写分类和备注后保存

#### 点击已记录格子

- 右侧显示「记录详情」
- 显示当前点击格子的时间段
- 显示所属完整记录的时间块
- 显示分类和备注内容
- 提供「编辑记录」和「删除记录」按钮

#### 拖拽包含已记录格子

- 如果拖拽范围包含已有记录，显示冲突提示
- 不允许直接覆盖，避免误操作
- 用户需要先删除或编辑已有记录

### 16.7 右侧面板三种状态

#### 状态 A：未选择（默认）

```text
选择时间块

点击或拖拽左侧格子，创建一条时间记录。
点击已有颜色的格子，可以查看完整记录。
```

#### 状态 B：新建记录

```text
新建记录

选择范围
09:00 - 10:15
共 1 小时 15 分钟

分类
[ 工作 ▼ ]

备注
[ 完成 Time Grid 交互设计 ]

[保存记录]
[清除选择]
```

#### 状态 C：记录详情

```text
记录详情

当前格子
09:15 - 09:30

所属记录
09:00 - 09:30
共 30 分钟

分类
学习

备注
看书

[编辑记录]
[删除记录]
```

点击「编辑记录」后切换为可编辑状态，允许修改分类和备注。

### 16.8 多用户切换模拟

右上角提供简单的用户切换功能，用于模拟多用户场景。

设计方案：

```text
Alex   Mina   Kai
```

- 当前用户按钮高亮
- 其他用户为浅灰按钮
- 点击切换用户后，网格数据和记录列表随之切换
- 每个用户维护独立的模拟记录

原型中会预置少量示例记录，让用户能够直观感受多用户数据隔离效果。

### 16.9 记录块边界识别

为了让用户清楚地识别哪些格子属于同一条记录，原型会提供以下视觉反馈：

- 同一记录块的所有格子使用相同颜色
- hover 任意一个格子时，同一记录块的所有格子出现外圈高亮
- 点击后，整条记录覆盖的格子加上 ring 边框

这样用户能明显感觉到「这是一个连续时间块」。

### 16.10 时间显示规则

每个格子对应一个 `slotIndex`：

```text
slotIndex = 行号 * 4 + 列号
```

例如：

```text
行 9，列 1
slotIndex = 9 * 4 + 1 = 37
时间段 = 09:15 - 09:30
```

每条记录包含：

```text
startIndex: 36
endIndex: 38
```

这里 `endIndex` 是结束边界，不包含自身。

所以：

```text
startIndex 36 = 09:00
endIndex 38 = 09:30
```

覆盖格子：

```text
36: 09:00 - 09:15
37: 09:15 - 09:30
```

右侧展示：

```text
当前格子：09:15 - 09:30
所属时间块：09:00 - 09:30
```

### 16.11 原型技术实现

原型使用以下技术栈：

- Tailwind CSS CDN（样式）
- Vue 3 CDN（交互逻辑）
- 单文件 HTML（无需构建工具）
- 可直接双击打开

Vue 数据结构：

```javascript
{
  users: ['Alex', 'Mina', 'Kai'],
  currentUser: 'Alex',
  records: {
    'Alex': [...],
    'Mina': [...],
    'Kai': [...]
  },
  selectedCells: [],
  isDragging: false,
  panelState: 'idle', // 'idle' | 'creating' | 'viewing'
  currentRecord: null
}
```

核心方法：

- `handleMouseDown(cellIndex)` - 开始选择
- `handleMouseEnter(cellIndex)` - 拖拽扩展
- `handleMouseUp()` - 结束选择
- `handleCellClick(cellIndex)` - 点击格子
- `saveRecord()` - 保存记录
- `deleteRecord()` - 删除记录
- `switchUser(username)` - 切换用户
- `formatTime(slotIndex)` - 格式化时间

### 16.12 原型文件位置

```text
docs/prototypes/time-grid-v1.html
```

直接在浏览器中打开即可查看效果。

### 16.13 设计验证目标

通过该原型，我们希望验证：

1. 24 行 x 4 列布局是否符合用户认知
2. 点击和拖拽交互是否流畅
3. 记录块模型是否清晰易懂
4. 颜色区分是否有效
5. 右侧面板状态切换是否合理
6. 多用户切换是否直观

原型验证通过后，将作为前端实现的参考标准。

## 17. 附录：Repository 查询优化

### 17.1 TimeSlotRepository

```java
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    
    /**
     * 查询用户某日的所有时间块（带分类信息，避免 N+1 问题）
     */
    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.userId = :userId AND ts.date = :date ORDER BY ts.slotIndex")
    List<TimeSlot> findByUserIdAndDateWithCategory(
        @Param("userId") Long userId, 
        @Param("date") LocalDate date
    );
    
    /**
     * 统计某个分类被使用的次数
     */
    @Query("SELECT COUNT(ts) FROM TimeSlot ts WHERE ts.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * 检查某个分类是否被使用
     */
    @Query("SELECT CASE WHEN COUNT(ts) > 0 THEN true ELSE false END FROM TimeSlot ts WHERE ts.category.id = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);
}
```

### 17.2 CategoryRepository

```java
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * 查询用户的所有活跃分类
     */
    @Query("SELECT c FROM Category c WHERE c.userId = :userId AND c.status = 'ACTIVE' ORDER BY c.displayOrder")
    List<Category> findActiveByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户的某个分类（用于权限校验）
     */
    @Query("SELECT c FROM Category c WHERE c.userId = :userId AND c.id = :id")
    Optional<Category> findByUserIdAndId(@Param("userId") Long userId, @Param("id") Long id);
    
    /**
     * 检查用户是否已有同名的活跃分类
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE c.userId = :userId AND c.name = :name AND c.status = 'ACTIVE'")
    boolean existsActiveByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
    
    /**
     * 获取用户当前最大的 displayOrder（用于新建分类时自动排序）
     */
    @Query("SELECT COALESCE(MAX(c.displayOrder), -1) FROM Category c WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    Integer findMaxDisplayOrderByUserId(@Param("userId") Long userId);
}
```

### 17.3 DTO 设计

#### TimeSlotDto

```java
public record TimeSlotDto(
    Integer slotIndex,
    String activityName,
    Long categoryId,
    String categoryName,
    String categoryColor,
    String categoryStatus,
    String displayName
) {
    public TimeSlotDto {
        if (slotIndex == null || slotIndex < 0 || slotIndex > 95) {
            throw new IllegalArgumentException("slotIndex must be between 0 and 95");
        }
        if (activityName == null || activityName.isBlank()) {
            throw new IllegalArgumentException("activityName cannot be blank");
        }
        
        // 自动计算 displayName
        displayName = "ARCHIVED".equals(categoryStatus) 
            ? categoryName + " (已归档)" 
            : categoryName;
    }
    
    /**
     * 从 TimeSlot 实体转换为 DTO
     */
    public static TimeSlotDto fromEntity(TimeSlot timeSlot) {
        Category category = timeSlot.getCategory();
        return new TimeSlotDto(
            timeSlot.getSlotIndex(),
            timeSlot.getActivityName(),
            category.getId(),
            category.getName(),
            category.getColorCode(),
            category.getStatus().name(),
            null // displayName 由构造器自动计算
        );
    }
}
```

#### CategoryDto

```java
public record CategoryDto(
    Long id,
    String name,
    String colorCode,
    String status,
    Integer displayOrder
) {
    public CategoryDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (colorCode == null || !colorCode.matches("^#[0-9a-fA-F]{6}$")) {
            throw new IllegalArgumentException("colorCode must be a valid hex color");
        }
    }
}
```

#### DeleteCategoryResponseDto

```java
public record DeleteCategoryResponseDto(
    String action,        // "DELETED" | "ARCHIVED"
    Long affectedRecords  // 受影响的历史记录数量
) {
    public DeleteCategoryResponseDto {
        if (action == null || (!action.equals("DELETED") && !action.equals("ARCHIVED"))) {
            throw new IllegalArgumentException("action must be DELETED or ARCHIVED");
        }
        if (affectedRecords == null || affectedRecords < 0) {
            throw new IllegalArgumentException("affectedRecords must be non-negative");
        }
    }
}
```
