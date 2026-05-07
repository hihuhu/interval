# TDD 实现总结：用户登录功能（JWT 方案）

## 实施日期
2026-05-07

## TDD 三步骤完成情况

### ✅ Step 1: Red（编写失败的测试）

**测试文件**: `AuthServiceTest.java`

包含 6 个核心测试用例：
1. ✅ `should_return_jwt_when_login_success` - 登录成功返回 JWT token
2. ✅ `should_throw_exception_when_username_not_exists` - 用户名不存在抛出异常
3. ✅ `should_throw_exception_when_password_incorrect` - 密码错误抛出异常
4. ✅ `should_return_user_info_when_register_success` - 注册成功返回用户信息
5. ✅ `should_throw_exception_when_username_already_exists` - 用户名已存在抛出异常
6. ✅ `should_throw_exception_when_password_too_weak` - 密码强度不足抛出异常

---

### ✅ Step 2: Green（编写最简实现）

#### 依赖管理
- 添加 Spring Security
- 添加 JWT (jjwt 0.12.3)
- 添加 BCrypt 密码加密

#### 核心实现类

**实体层**
- `User.java` - 用户实体（id, username, passwordHash）

**数据访问层**
- `UserRepository.java` - JPA Repository

**DTO 层**
- `LoginResponseDto.java` - 登录响应
- `RegisterResponseDto.java` - 注册响应

**服务层**
- `AuthService.java` - 认证服务接口
- `AuthServiceImpl.java` - 认证服务实现

**工具类**
- `JwtUtil.java` - JWT token 生成和验证
- `PasswordEncoderConfig.java` - BCrypt 配置

**配置类**
- `SecurityConfig.java` - Spring Security 配置
- `IntervalApplication.java` - Spring Boot 主类
- `application.properties` - 应用配置

---

### ✅ Step 3: Refactor（完善与重构）

#### 3.1 自定义异常体系

创建了三个专用异常类，提高错误处理的精确性：

```
AuthenticationFailedException - 认证失败（401）
UserAlreadyExistsException - 用户已存在（409）
PasswordValidationException - 密码验证失败（400）
```

#### 3.2 单一职责原则

**PasswordValidator.java**
- 提取密码验证逻辑到独立组件
- 提供 `validate()` 和 `isValid()` 方法
- 密码规则：至少 8 位，包含字母和数字

#### 3.3 增强的 AuthServiceImpl

**改进点**：
- ✅ 添加 SLF4J 日志记录
- ✅ 输入参数验证（空值、空白检查）
- ✅ 用户名格式验证（3-50 字符，仅字母数字下划线连字符）
- ✅ 使用自定义异常替代通用 IllegalArgumentException
- ✅ 依赖注入 PasswordValidator

#### 3.4 统一响应格式

**ApiResponse.java**
- 标准响应结构：`{ result, message, data }`
- 静态工厂方法：`success()`, `error()`

**GlobalExceptionHandler.java**
- 统一异常处理
- 映射异常到合适的 HTTP 状态码
- 返回标准 ApiResponse 格式

#### 3.5 REST API 层

**AuthController.java**
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册

**请求 DTO**
- `LoginRequest.java` - 登录请求（带验证）
- `RegisterRequest.java` - 注册请求（带验证）

#### 3.6 测试增强

**更新 AuthServiceTest.java**
- 适配新的自定义异常类型
- Mock PasswordValidator
- 验证异常类型和消息

**新增 PasswordValidatorTest.java**
- 参数化测试（@ParameterizedTest）
- 测试有效密码场景
- 测试各种无效密码场景（太短、只有字母、只有数字、空值）

---

## 最终项目结构

```
interval-server/
├── src/
│   ├── main/
│   │   ├── java/com/interval/
│   │   │   ├── auth/
│   │   │   │   ├── AuthController.java              ⭐ REST API
│   │   │   │   ├── AuthService.java                 ⭐ 服务接口
│   │   │   │   ├── AuthServiceImpl.java             ⭐ 服务实现
│   │   │   │   ├── AuthenticationFailedException.java
│   │   │   │   ├── UserAlreadyExistsException.java
│   │   │   │   ├── PasswordValidationException.java
│   │   │   │   ├── PasswordValidator.java           ⭐ 密码验证器
│   │   │   │   ├── PasswordEncoderConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtUtil.java                     ⭐ JWT 工具
│   │   │   │   ├── User.java                        ⭐ 实体
│   │   │   │   ├── UserRepository.java              ⭐ 数据访问
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LoginResponseDto.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── RegisterResponseDto.java
│   │   │   ├── common/
│   │   │   │   ├── ApiResponse.java                 ⭐ 统一响应
│   │   │   │   └── GlobalExceptionHandler.java      ⭐ 全局异常处理
│   │   │   └── IntervalApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/interval/auth/
│           ├── AuthServiceTest.java                 ⭐ 核心测试
│           └── PasswordValidatorTest.java           ⭐ 验证器测试
└── build.gradle
```

---

## 核心功能实现

### 1. 用户注册流程

```
1. 接收 RegisterRequest (username, password)
2. 验证用户名格式（3-50字符，字母数字下划线连字符）
3. 检查用户名是否已存在 → UserAlreadyExistsException
4. 验证密码强度 → PasswordValidationException
5. BCrypt 加密密码
6. 保存用户到数据库
7. 返回 RegisterResponseDto (userId, username)
```

### 2. 用户登录流程

```
1. 接收 LoginRequest (username, password)
2. 验证输入非空
3. 查找用户 → AuthenticationFailedException (用户不存在)
4. BCrypt 验证密码 → AuthenticationFailedException (密码错误)
5. 生成 JWT token
6. 返回 LoginResponseDto (token, username)
```

### 3. JWT Token 结构

```json
{
  "userId": 1,
  "username": "alex",
  "sub": "alex",
  "iat": 1715097600,
  "exp": 1715184000
}
```

---

## 遵循的设计原则

### SOLID 原则
- ✅ **单一职责**：PasswordValidator 独立处理密码验证
- ✅ **开闭原则**：通过接口和继承支持扩展
- ✅ **依赖倒置**：依赖接口而非具体实现

### DRY 原则
- ✅ 密码验证逻辑集中在 PasswordValidator
- ✅ 异常处理统一在 GlobalExceptionHandler
- ✅ 响应格式统一在 ApiResponse

### KISS 原则
- ✅ 简单直接的登录/注册逻辑
- ✅ 清晰的异常层次结构

### YAGNI 原则
- ✅ 只实现当前需要的功能
- ✅ 避免过度设计

---

## 安全最佳实践

### OWASP 合规
- ✅ 密码使用 BCrypt 加密存储
- ✅ 密码强度验证（最少 8 位，字母+数字）
- ✅ 登录失败不泄露具体原因（统一返回 "Invalid username or password"）
- ✅ JWT token 有过期时间（24 小时）
- ✅ 输入验证防止注入攻击

---

## API 端点文档

### POST /api/auth/register

**请求体**:
```json
{
  "username": "alex",
  "password": "SecurePass123!"
}
```

**成功响应** (201 Created):
```json
{
  "result": "SUCCESS",
  "message": "Registration successful",
  "data": {
    "userId": 1,
    "username": "alex"
  }
}
```

**错误响应** (409 Conflict):
```json
{
  "result": "ERROR",
  "message": "Username already exists",
  "data": null
}
```

---

### POST /api/auth/login

**请求体**:
```json
{
  "username": "alex",
  "password": "SecurePass123!"
}
```

**成功响应** (200 OK):
```json
{
  "result": "SUCCESS",
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "alex"
  }
}
```

**错误响应** (401 Unauthorized):
```json
{
  "result": "ERROR",
  "message": "Invalid username or password",
  "data": null
}
```

---

## 测试覆盖率

### AuthServiceTest (6 个测试用例)
- ✅ 登录成功场景
- ✅ 用户名不存在场景
- ✅ 密码错误场景
- ✅ 注册成功场景
- ✅ 用户名已存在场景
- ✅ 密码强度不足场景

### PasswordValidatorTest (7 个测试用例)
- ✅ 有效密码（5 个参数化测试）
- ✅ 密码太短（3 个参数化测试）
- ✅ 只有字母（3 个参数化测试）
- ✅ 只有数字（3 个参数化测试）
- ✅ 空密码
- ✅ 空白密码

**总计**: 13 个测试用例

---

## 下一步建议

### 功能增强
1. 添加 JWT token 刷新机制
2. 实现用户登出（token 黑名单）
3. 添加邮箱验证
4. 实现忘记密码功能
5. 添加用户角色和权限管理

### 安全增强
1. 添加登录失败次数限制（防暴力破解）
2. 实现 CAPTCHA 验证
3. 添加 IP 白名单/黑名单
4. 实现双因素认证（2FA）

### 测试增强
1. 添加集成测试（使用 @SpringBootTest）
2. 添加 Controller 层测试（使用 MockMvc）
3. 添加性能测试

---

## 总结

通过 TDD 三步骤（Red → Green → Refactor），我们成功实现了：

✅ **完整的用户认证系统**（注册、登录、JWT）  
✅ **清晰的代码结构**（分层架构、单一职责）  
✅ **健壮的错误处理**（自定义异常、全局处理器）  
✅ **全面的测试覆盖**（单元测试、参数化测试）  
✅ **安全最佳实践**（BCrypt、JWT、输入验证）  
✅ **统一的 API 规范**（ApiResponse、RESTful）  

代码质量高，可维护性强，符合企业级开发标准。
