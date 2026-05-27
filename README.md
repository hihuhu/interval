# Interval

Interval 是一个时间块记录与复盘工具。它把一天拆成 96 个 15 分钟格子，让用户用分类记录当天做了什么，再通过统计页回看时间花在了哪里。

这个项目适合个人时间追踪、专注复盘、学习/工作投入分析，也可以作为 Vue 3 + Spring Boot 前后端分离应用的完整示例。

## 它能做什么

- **账号体系**：注册、登录、JWT 鉴权、受保护路由。
- **时间格记录**：按天查看 96 个 15 分钟格子，点击或拖拽选择时间块。
- **批量登记**：一次给连续或跳选的时间块设置分类和备注。
- **分类管理**：创建、编辑、删除分类；已有历史记录的分类会自动归档而不是破坏历史数据。
- **统计复盘**：按今日、本周、本月或自定义范围查看分类耗时、占比、记录率和未记录时间。
- **用户隔离**：后端所有分类、时间格和统计数据都按当前登录用户隔离。
- **数据库可切换**：默认 H2 零配置启动，后续可通过 `mysql` profile 切换到 MySQL。

## 项目结构

```text
Interval/
├── interval-client/   # Vue 3 + TypeScript 前端
├── interval-server/   # Spring Boot 3 + Java 17 后端
├── docs/design/       # 系统设计与功能 SDD
├── docs/prototypes/   # HTML 原型
├── PROJECT_LOG.md     # 开发记录
└── QUICK_START.md     # 快速接手指南
```

## 技术栈

后端：

- Java 17
- Spring Boot 3
- Spring Web / Data JPA / Security / Validation
- JWT (`jjwt`)
- H2，MySQL 可选 profile
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

```bash
cd interval-server
./gradlew bootRun
```

Windows 也可以使用：

```powershell
cd interval-server
.\gradlew.bat bootRun
```

后端默认地址：

```text
http://localhost:8088
```

默认使用内存 H2，无需安装数据库。H2 Console：

```text
http://localhost:8088/h2-console
```

默认开发账号：

```text
admin / 123ABCdef*
```

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

## MySQL 切换

默认启动仍然使用 H2。安装 MySQL 后，创建数据库并启用 `mysql` profile 即可切换。

PowerShell 示例：

```powershell
cd interval-server
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:MYSQL_JDBC_URL = "jdbc:mysql://localhost:3306/interval?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:MYSQL_USERNAME = "interval"
$env:MYSQL_PASSWORD = "interval"
.\gradlew.bat bootRun
```

可用变量模板：

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

## 本地体验流程

1. 启动后端和前端。
2. 打开 `http://localhost:5173`。
3. 用默认账号登录，或注册新账号。
4. 在时间格页面选择格子，设置分类和备注。
5. 进入统计页，切换今日、本周、本月或自定义范围查看分类耗时。
6. 在分类管理中删除已使用分类，观察归档分类仍出现在历史统计中。

## 开发约定

- 重要功能先写设计文档，再写测试，再实现。
- 后端接口统一返回 `ApiResponse<T>`。
- 前端 API 调用统一放在 `src/services/`。
- 后端 Controller 不直接访问 Repository，业务逻辑放在 Service 层。
- 提交信息使用 Conventional Commits。

## 相关文档

- [快速接手指南](QUICK_START.md)
- [项目开发日志](PROJECT_LOG.md)
- [系统设计](docs/design/system-design.md)
- [统计功能设计](docs/design/sdd-category-duration-stats.md)
- [原型目录](docs/prototypes/README.md)
