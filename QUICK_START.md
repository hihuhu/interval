# 快速开始指南

> 新窗口必读。请先阅读 `PROJECT_LOG.md` 了解最新历史和决策。

---

## 必读文件

1. `PROJECT_LOG.md` — 操作历史、技术决策、待办事项，按时间倒序排列。
2. `docs/design/system-design.md` — 主系统设计文档。
3. `docs/design/sdd-frontend-auth-time-grid.md` — 前端认证与 Time Grid 基础设计。
4. `docs/design/sdd-frontend-category-range-extension.md` — 分类管理与范围填充扩展设计。
5. `README.md` — 当前运行、测试和项目概览。

---

## 当前项目状态

### 已完成

- 后端认证模块：注册、登录、BCrypt、JWT。
- 后端 JWT 用户上下文：Category 和 TimeSlot API 通过 `Authorization: Bearer <token>` 获取当前用户。
- 后端分类模块：查询、新建、更新、智能删除（无历史记录物理删除，有历史记录归档）。
- 后端 TimeSlot 模块：每日查询、单格 upsert、删除。
- 前端基础架构：Vue 3 + TypeScript + Vite + Router + Pinia + Axios。
- 前端认证闭环：登录、注册、JWT 保存、路由守卫、401 清理登录态。
- 前端 Time Grid：96 个 15 分钟格、单格创建/编辑/删除。
- 前端分类管理：查询、新建、编辑、删除/归档结果提示。
- 前端范围填充：先点击起点，再 Shift-click 终点，批量填充连续格子。
- 前后端 API 冒烟验证通过。
- 前端测试和生产构建通过。
- 后端测试通过。

### 当前端口

- 后端：`http://localhost:8088`
- 前端：`http://localhost:5173`
- H2 Console：`http://localhost:8088/h2-console`
- 默认数据库：内存 H2，无需安装数据库。
- 可选数据库：MySQL profile，后续安装 MySQL 后启用 `SPRING_PROFILES_ACTIVE=mysql` 即可切换。

---

## 常用命令

### 后端

```bash
cd interval-server
gradle test
gradle bootRun
```

默认启动使用 H2。切换 MySQL 时先创建数据库，再设置环境变量：

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:MYSQL_JDBC_URL = "jdbc:mysql://localhost:3306/interval?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:MYSQL_USERNAME = "interval"
$env:MYSQL_PASSWORD = "interval"
gradle bootRun
```

变量模板见 `interval-server/.env.mysql.example`。

如果 Gradle Wrapper 下载超时，可使用本机系统 Gradle。此前本机可用：

```text
F:\app\gradle-9.5.1\bin\gradle.bat
```

### 前端

```bash
cd interval-client
npm install
npm run test
npm run build
npm run dev
```

前端环境变量模板：

```bash
VITE_API_BASE_URL=http://localhost:8088
```

---

## 本地联调步骤

1. 启动后端：

```bash
cd interval-server
gradle bootRun
```

2. 启动前端：

```bash
cd interval-client
npm run dev
```

3. 打开：`http://localhost:5173`
4. 注册新用户。
5. 登录。
6. 新建分类。
7. 编辑分类名称或颜色。
8. 点击单个时间格创建记录。
9. 先点击一个格子，再按住 Shift 点击另一个格子，批量填充连续格。
10. 删除时间格。
11. 删除分类并查看 `DELETED` 或 `ARCHIVED` 提示。

---

## 关键目录

```text
interval/
├── docs/
│   ├── design/
│   │   ├── system-design.md
│   │   ├── sdd-frontend-auth-time-grid.md
│   │   ├── sdd-frontend-category-range-extension.md
│   │   ├── sdd-jwt-user-context.md
│   │   └── sdd-timeslot-module.md
│   └── prototypes/
├── interval-server/
│   ├── src/main/java/com/interval/
│   │   ├── auth/
│   │   ├── category/
│   │   ├── timeslot/
│   │   └── common/
│   └── src/test/java/com/interval/
├── interval-client/
│   ├── src/components/
│   ├── src/views/
│   ├── src/stores/
│   ├── src/services/
│   ├── src/types/
│   └── src/__tests__/
├── PROJECT_LOG.md
├── QUICK_START.md
└── README.md
```

---

## 开发规则摘要

- 新功能或非平凡变更必须先更新 `docs/design/`。
- 后端新功能：先 JUnit 5 测试，再实现。
- 前端新功能：先 Vitest / Vue Test Utils 测试，再实现。
- 所有 API 响应使用 `ApiResponse<T>`。
- 前端不能直接调用 Axios，必须通过 `src/services/`。
- 后端 Controller 不直接访问 Repository，业务逻辑在 ServiceImpl。
- 每次会话结束前更新 `PROJECT_LOG.md`。

---

## 当前后续可选增强

- 设计并实现后端批量 TimeSlot API，替代前端循环 upsert。
- 分类排序 UI。
- 归档分类恢复功能。
- 时间统计/日报/周报视图。
- JWT refresh token 与生产环境密钥管理。
- 生产 CORS 域名配置。
