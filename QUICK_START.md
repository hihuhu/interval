# 快速开始指南

> 新窗口必读！快速了解项目状态和如何开始工作

---

## 📖 必读文件（按顺序阅读）

### 1. **PROJECT_LOG.md** ⭐ 最重要！
- 记录所有操作历史、技术决策、待办事项
- 时间倒序排列，最新的在最上面
- **每次会话必须先读这个文件**

### 2. **WORK_SUMMARY.md**
- 当前工作状态的详细总结
- 已完成的功能清单
- 测试用例概览

### 3. **docs/design/system-design.md**
- 完整的系统设计文档
- 数据模型、API 契约、业务规则

### 4. **README.md**
- 项目基本信息
- 技术栈说明

---

## 🎯 当前项目状态

### 阶段
✅ **认证模块**：已完成设计、测试、实现和验证  
✅ **分类管理模块**：已完成设计、测试、实现和验证  
⏳ **时间块模块**：待开始  
⏳ **前端实现**：待开始

### 最后更新
2026-05-08 - 认证模块和分类管理模块实现完成并验证通过

### 下一步
1. 修复 JUnit 测试运行问题
2. 实现时间块管理模块（TimeSlot Entity, Service, Controller）
3. 集成 JWT 认证到所有 API
4. 开始前端 Vue 项目开发

---

## 🚀 常用命令

### 后端（interval-server）

```bash
# 进入后端目录
cd interval-server

# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests AuthServiceTest

# 启动后端服务
./gradlew bootRun

# 清理构建
./gradlew clean

# 构建项目
./gradlew build
```

**Windows 用户**：使用 `gradlew.bat` 替代 `./gradlew`

### 前端（interval-client）

```bash
# 进入前端目录
cd interval-client

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 运行测试
npm run test
```

---

## 📁 关键目录结构

```
D:\Project2\Interval
├── docs/
│   ├── design/              # 系统设计文档
│   │   ├── system-design.md           # 主设计文档 ⭐
│   │   ├── implementation-report.md   # 实现报告
│   │   └── tdd-auth-implementation-summary.md
│   ├── prototypes/          # UI 原型
│   │   ├── login.html                 # 登录页面原型 ✅
│   │   ├── time-grid-v1.html          # Time Grid 原型
│   │   └── UPDATE_INSTRUCTIONS.md
│   └── gradle-setup.md      # Gradle 配置指南
│
├── interval-server/         # 后端（Spring Boot 3 + Java 17）
│   ├── src/
│   │   ├── main/java/com/interval/
│   │   │   ├── auth/        # 认证模块 ✅
│   │   │   ├── common/      # 公共类（ApiResponse, GlobalExceptionHandler）
│   │   │   └── IntervalApplication.java
│   │   ├── main/resources/
│   │   │   └── application.properties
│   │   └── test/java/com/interval/
│   │       ├── auth/        # 认证模块测试 ✅
│   │       └── category/    # 分类模块测试 ⏳
│   ├── build.gradle         # Gradle 配置
│   ├── TEST_GUIDE.md        # 测试指南
│   └── VERIFICATION_REPORT.md
│
├── interval-client/         # 前端（Vue 3 + TypeScript）⏳
│   └── .cursor/rules/       # 前端开发规则
│
├── PROJECT_LOG.md           # 操作日志 ⭐ 必读！
├── QUICK_START.md           # 本文件
├── WORK_SUMMARY.md          # 工作总结
└── README.md                # 项目说明
```

---

## 🔧 技术栈

### 后端
- **框架**：Spring Boot 3.x
- **语言**：Java 17
- **构建工具**：Gradle
- **数据库**：H2（开发/测试）
- **认证**：JWT + BCrypt
- **测试**：JUnit 5
- **架构**：RESTful API（前后端分离）

### 前端
- **框架**：Vue 3（Composition API）
- **语言**：TypeScript（strict mode）
- **构建工具**：Vite
- **状态管理**：Pinia
- **路由**：Vue Router 4
- **HTTP 客户端**：Axios
- **测试**：Vitest + Vue Test Utils

---

## 📋 开发工作流

### 标准流程（遵循 .cursorrules）
1. **设计阶段**：更新 `docs/design/` 中的设计文档
2. **测试阶段**：编写 JUnit 5 测试用例（TDD）
3. **实现阶段**：实现功能代码
4. **验证阶段**：运行测试，手动验证
5. **记录阶段**：更新 `PROJECT_LOG.md`

### 每次会话开始
1. ✅ 阅读 `PROJECT_LOG.md` 了解最新进展
2. ✅ 检查待办事项列表
3. ✅ 确认当前要做的任务

### 每次会话结束
1. ✅ 更新 `PROJECT_LOG.md` 记录本次操作
2. ✅ 更新待办事项状态
3. ✅ 记录技术决策和遇到的问题

---

## 🎓 重要规则

### 来自 .cursorrules
- **设计优先**：任何功能必须先更新设计文档
- **测试驱动**：先写测试，再写实现
- **前后端分离**：后端只提供 RESTful JSON API
- **数据隔离**：多用户系统，所有数据必须按 userId 隔离
- **安全第一**：遵循 OWASP 最佳实践

### Entity 类规范
- 使用 `@Entity` 和 `@Data`（Lombok）
- ID 使用 `@Id` + `@GeneratedValue(strategy=IDENTITY)`
- 关系使用 `FetchType.LAZY`

### DTO 规范
- 使用 `record` 类型
- 必须有 compact canonical constructor 验证输入

### Service 规范
- Service 是 interface，实现在 ServiceImpl
- ServiceImpl 使用 `@Service` 注解
- 返回 DTO，不返回 Entity

### Controller 规范
- 使用 `@RestController` 和 `@RequestMapping`
- 返回 `ResponseEntity<ApiResponse<?>>`
- 所有逻辑在 try-catch 中，异常交给 GlobalExceptionHandler

---

## 🆘 常见问题

### Q: 如何运行测试？
```bash
cd interval-server
./gradlew test
```

### Q: 如何启动后端？
```bash
cd interval-server
./gradlew bootRun
```
默认端口：8080

### Q: 如何查看 H2 数据库？
启动后端后访问：http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (留空)

### Q: 测试失败怎么办？
1. 查看 `interval-server/TEST_GUIDE.md`
2. 查看 `interval-server/VERIFICATION_REPORT.md`
3. 检查 `PROJECT_LOG.md` 中的已知问题

### Q: 如何添加新功能？
1. 先更新 `docs/design/system-design.md`
2. 编写测试用例
3. 实现功能代码
4. 运行测试验证
5. 更新 `PROJECT_LOG.md`

---

## 📞 获取帮助

### 文档资源
- **系统设计**：`docs/design/system-design.md`
- **测试指南**：`interval-server/TEST_GUIDE.md`
- **验证报告**：`interval-server/VERIFICATION_REPORT.md`
- **Gradle 配置**：`docs/gradle-setup.md`
- **原型更新**：`docs/prototypes/UPDATE_INSTRUCTIONS.md`

### 关键概念
- **User**：用户实体，包含 username、password（加密）、email
- **Category**：分类实体，每个用户独立的分类体系
- **TimeSlot**：时间块实体，记录用户的时间使用
- **JWT**：无状态认证，24 小时过期
- **BCrypt**：密码加密，强度因子 12

---

## ✅ 检查清单

### 开始工作前
- [ ] 已阅读 `PROJECT_LOG.md`
- [ ] 已了解当前项目状态
- [ ] 已确认下一步任务
- [ ] 已检查相关设计文档

### 完成工作后
- [ ] 所有测试通过
- [ ] 代码符合规范
- [ ] 已更新 `PROJECT_LOG.md`
- [ ] 已更新待办事项

---

**最后更新**：2026-05-08  
**项目状态**：认证模块完成，分类模块待实现  
**下一步**：实现分类管理模块
