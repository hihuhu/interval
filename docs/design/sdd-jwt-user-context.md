# JWT 用户上下文集成 SDD

## 1. Overview

本设计用于把后端 Category 与 TimeSlot API 当前临时使用的 `X-User-Id` Header 替换为标准 JWT 用户上下文。

现状：

- 登录接口已经生成 JWT token。
- token 中包含 `userId` 与 `username` claims。
- Spring Security 当前配置为所有请求 `permitAll`。
- Category 与 TimeSlot Controller 通过 `@RequestHeader("X-User-Id")` 获取用户 ID。

目标：

- 客户端登录后通过 `Authorization: Bearer <token>` 调用受保护 API。
- 后端从 JWT 中解析当前用户 ID，并注入到 Controller 参数。
- Category 与 TimeSlot 不再接受 `X-User-Id` 作为用户身份来源。
- 未认证或 token 无效时返回统一 `ApiResponse` 错误结构。
- 保持 Service 层方法签名不变，继续显式接收 `Long userId`，避免业务层依赖 Web/Security 上下文。

不在本阶段范围：

- Refresh Token。
- 登出与 token 黑名单。
- 角色/权限模型。
- 前端登录态持久化实现。
- 迁移到生产级密钥管理。

## 2. API Contract

### 2.1 Public APIs

以下接口公开访问，不要求 JWT：

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/auth/register` | 用户注册 |
| `POST` | `/api/auth/login` | 用户登录并返回 JWT |
| `GET` | `/h2-console/**` | 本地开发数据库控制台 |

### 2.2 Protected APIs

以下接口必须携带 JWT：

| Method | Path | Previous Auth | New Auth |
| --- | --- | --- | --- |
| `GET` | `/api/categories` | `X-User-Id` | `Authorization: Bearer <token>` |
| `POST` | `/api/categories` | `X-User-Id` | `Authorization: Bearer <token>` |
| `PUT` | `/api/categories/{categoryId}` | `X-User-Id` | `Authorization: Bearer <token>` |
| `DELETE` | `/api/categories/{categoryId}` | `X-User-Id` | `Authorization: Bearer <token>` |
| `GET` | `/api/time-slots` | `X-User-Id` | `Authorization: Bearer <token>` |
| `PUT` | `/api/time-slots` | `X-User-Id` | `Authorization: Bearer <token>` |
| `DELETE` | `/api/time-slots/{slotId}` | `X-User-Id` | `Authorization: Bearer <token>` |

### 2.3 Request Header

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Rules:

- Header name is case-insensitive as defined by HTTP.
- Scheme must be `Bearer`.
- Token must be a valid JWT signed by the configured `jwt.secret`.
- Token must not be expired.
- Token must include a numeric `userId` claim.

### 2.4 Error Responses

All authentication failures use the existing `ApiResponse` envelope.

#### Missing token

Status: `401 UNAUTHORIZED`

```json
{
  "result": "ERROR",
  "message": "Authentication required",
  "data": null
}
```

#### Invalid or expired token

Status: `401 UNAUTHORIZED`

```json
{
  "result": "ERROR",
  "message": "Invalid or expired token",
  "data": null
}
```

#### Authenticated user no longer exists

Status: `401 UNAUTHORIZED`

```json
{
  "result": "ERROR",
  "message": "Authenticated user not found",
  "data": null
}
```

## 3. Architecture

```text
HTTP Request
  -> JwtAuthenticationFilter
    -> JwtUtil parses and validates token
    -> UserRepository verifies user still exists
    -> SecurityContext stores AuthenticatedUser principal
  -> Controller receives @AuthenticationPrincipal AuthenticatedUser
  -> Controller calls service with principal.userId()
  -> Service keeps existing user-isolation rules
```

## 4. Component Design

### 4.1 AuthenticatedUser

Package: `com.interval.auth.security`

```java
public record AuthenticatedUser(Long userId, String username) {
}
```

Purpose:

- Represents the current authenticated user in Spring Security.
- Keeps Controller code independent from raw JWT claims.
- Avoids using `User` entity as security principal.

### 4.2 JwtUtil Extensions

Existing `JwtUtil` will be extended with:

- `Long getUserIdFromToken(String token)`
- optional internal helper for parsing claims to avoid duplicate parser setup

Existing methods remain compatible:

- `generateToken(Long userId, String username)`
- `getUsernameFromToken(String token)`
- `validateToken(String token)`

### 4.3 JwtAuthenticationFilter

Package: `com.interval.auth.security`

Responsibilities:

- Run once per request using `OncePerRequestFilter`.
- Skip public endpoints:
  - `/api/auth/register`
  - `/api/auth/login`
  - `/h2-console/**`
- Read `Authorization` header.
- If the protected request has no Bearer token, return `401 Authentication required`.
- If token is invalid, expired, or missing `userId`, return `401 Invalid or expired token`.
- If token is valid but the user ID no longer exists, return `401 Authenticated user not found`.
- On success, put an `Authentication` into `SecurityContextHolder` with `AuthenticatedUser` as principal.

Recommended implementation details:

- Use `UsernamePasswordAuthenticationToken(principal, null, List.of())`.
- Do not assign roles for now.
- Write JSON error using `ObjectMapper` and `ApiResponse.error(message)`.
- Set response content type to `application/json`.

### 4.4 SecurityConfig

Update security configuration:

- Keep CSRF disabled for REST API and H2 console compatibility.
- Keep frame options disabled for H2 console local development.
- Permit only public endpoints.
- Require authentication for any other request.
- Register `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

Expected policy:

```text
/api/auth/register -> permitAll
/api/auth/login    -> permitAll
/h2-console/**     -> permitAll
any other request  -> authenticated
```

### 4.5 Controller Changes

Category and TimeSlot controllers will stop reading `X-User-Id`.

Before:

```java
@RequestHeader("X-User-Id") Long userId
```

After:

```java
@AuthenticationPrincipal AuthenticatedUser user
```

Controller extracts:

```java
Long userId = user.userId();
```

Service interfaces remain unchanged.

## 5. Test Plan

### 5.1 JwtUtilTest

Cover:

- Generated token contains username.
- Generated token contains userId.
- Valid generated token passes validation.
- Malformed token fails validation.

### 5.2 JwtAuthenticationFilterTest or Security Integration Tests

Minimum required coverage with MockMvc/SpringBootTest:

- `GET /api/categories` without token returns 401 and `Authentication required`.
- `GET /api/categories` with malformed token returns 401 and `Invalid or expired token`.
- `GET /api/categories` with valid token for existing user returns 200.
- `GET /api/categories` with valid token for deleted/nonexistent user returns 401 and `Authenticated user not found`.
- `POST /api/auth/register` remains accessible without token.
- `POST /api/auth/login` remains accessible without token.

### 5.3 Controller Migration Tests

Update existing controller tests:

- `CategoryControllerTest` should use `@WithMockUser` or `@AuthenticationPrincipal` setup equivalent only if testing controller in isolation.
- Prefer integration-style MockMvc tests for actual JWT behavior.
- Existing service tests remain unchanged.

### 5.4 Regression Tests

Run full backend test suite:

```bash
gradle test
```

Manual HTTP verification:

1. Register or create a user.
2. Login to get token.
3. Call Category API without token -> 401.
4. Call Category API with token -> success.
5. Create category with token.
6. Create/query/update/delete TimeSlot with token.
7. Verify `X-User-Id` is no longer needed.

## 6. Migration Notes

- During this change, `X-User-Id` is removed from Category and TimeSlot APIs.
- Existing local HTTP scripts must switch to login first, then pass `Authorization: Bearer <token>`.
- Frontend should store token after login and attach it to protected API requests.
- H2 in-memory database means manual HTTP verification should register/login in the same backend process where API calls are tested.

## 7. Risks and Mitigations

| Risk | Mitigation |
| --- | --- |
| Existing tests fail because security is now enabled | Update controller/integration tests to use JWT or explicitly import test security setup |
| Token valid but user deleted | Filter checks `UserRepository.existsById(userId)` and returns 401 |
| Inconsistent error envelope from Spring Security | Filter writes `ApiResponse` directly for auth failures |
| H2 console blocked | Permit `/h2-console/**` and disable frame options for local development |
| Frontend not ready for JWT header | Document migration and keep implementation isolated to backend for now |

## 8. Acceptance Criteria

- Protected Category and TimeSlot APIs no longer require or read `X-User-Id`.
- Protected APIs return 401 when `Authorization` header is missing or invalid.
- Valid JWT identifies the current user and preserves data isolation.
- Existing Auth login/register behavior still works.
- Category smart delete and TimeSlot behavior continue passing tests.
- Full backend test suite passes.
