# Interval

Interval 是一个时间块记录与复盘工具。它把一天拆成 96 个 15 分钟格子，帮助你快速记录每段时间做了什么，并通过统计页面回看时间花在了哪里。

这个项目也可以作为一个完整的前后端分离示例：前端使用 Vue 3 + TypeScript，后端使用 Spring Boot 3 + Java 17，包含认证、分类、时间块、统计和本地开发数据库配置。

## 当前能力

- **账号体系**：注册、登录、JWT 鉴权、受保护路由。
- **时间格记录**：按天查看 96 个 15 分钟时间块，支持单击、跳选和拖拽选择。
- **批量编辑**：一次给多个时间块设置分类和备注，支持混合选择时保留或覆盖备注。
- **已登记块擦除**：在右侧编辑面板中批量擦除当前选中的已登记时间块。
- **分类管理**：创建、编辑、删除分类；已有历史记录的分类会自动归档，避免破坏历史数据。
- **统计复盘**：按今日、本周、本月或自定义范围查看分类耗时、占比、记录率和未记录时间。
- **用户隔离**：后端分类、时间块和统计数据都按当前登录用户隔离。
- **数据库切换**：默认使用内存 H2，后续可通过 `mysql` profile 切换到 MySQL。

## 项目结构

```text
Interval/
├── interval-client/   # Vue 3 + TypeScript 前端
├── interval-server/   # Spring Boot 3 + Java 17 后端
├── docs/design/       # 系统设计与功能 SDD
├── docs/prototypes/   # HTML 交互原型
├── PROJECT_LOG.md     # 项目开发日志
└── QUICK_START.md     # 快速接手指南
```

## 技术栈

后端：

- Java 17
- Spring Boot 3
- Spring Web / Data JPA / Security / Validation
- JWT
- H2 / MySQL
- Gradle
- JUnit 5

前端：

- Vue 3 Composition API
- TypeScript
- Vite
- Pinia
- Vue Router
- Axios
- Vitest + Vue Test Utils + jsdom

## 快速启动

### 1. 启动后端

Windows：

```powershell
cd interval-server
.\gradlew.bat bootRun
```

macOS / Linux：

```bash
cd interval-server
./gradlew bootRun
```

后端默认地址：

```text
http://localhost:8088
```

H2 Console：

```text
http://localhost:8088/h2-console
```

默认开发账号：

```text
admin / 123ABCdef*
```

> 账号管理规划说明：管理员账号管理 V1 已完成原型与设计文档，计划改为系统无管理员时默认创建 `admin / 123456`，并要求首次登录修改密码。当前代码中的默认账号仍以实际实现为准；实现交接见 `docs/superpowers/plans/2026-06-03-admin-account-management.md`。

### 2. 启动前端

```bash
cd interval-client
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

前端 API 地址配置见 `interval-client/.env.example`：

```bash
VITE_API_BASE_URL=http://localhost:8088
```

## 本地体验流程

1. 启动后端和前端。
2. 打开 `http://localhost:5173`。
3. 使用默认账号登录，或注册新账号。
4. 进入时间格页面，选择一个或多个时间块。
5. 在右侧面板选择分类、填写备注并保存。
6. 选择已登记时间块，测试批量覆盖或擦除。
7. 进入统计页，查看不同日期范围下的分类耗时。
8. 在分类管理中删除已使用分类，观察历史记录中的归档分类表现。

## MySQL 切换

默认启动使用内存 H2，无需安装数据库。需要切换到 MySQL 时，先创建数据库，再启用 `mysql` profile。

PowerShell 示例：

```powershell
cd interval-server
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:MYSQL_JDBC_URL = "jdbc:mysql://localhost:3306/interval?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:MYSQL_USERNAME = "interval"
$env:MYSQL_PASSWORD = "interval"
.\gradlew.bat bootRun
```

变量模板：

```text
interval-server/.env.mysql.example
```

## 常用验证命令

后端：

```bash
cd interval-server
./gradlew test
```

前端：

```bash
cd interval-client
npm run test
npm run build
```

## 开发约定

- 新功能或非平凡变更先更新 `docs/design/`。
- 后端新功能先写 JUnit 5 测试，再实现。
- 前端新功能先写 Vitest / Vue Test Utils 测试，再实现。
- 后端接口统一返回 `ApiResponse<T>`。
- 前端 API 调用统一放在 `src/services/`。
- 后端 Controller 不直接访问 Repository，业务逻辑放在 Service 层。
- 每次会话结束前同步 `PROJECT_LOG.md`。

## 相关文档

- [快速接手指南](QUICK_START.md)
- [项目开发日志](PROJECT_LOG.md)
- [系统设计](docs/design/system-design.md)
- [Time Grid v2 UI 迭代记录](docs/design/time-grid-v2-ui-iteration-2026-05-27.md)
- [统计功能设计](docs/design/sdd-category-duration-stats.md)
- [原型目录](docs/prototypes/README.md)
