# 测试运行指南

## 项目状态

✅ **所有代码已实现完成**
- 16 个 Java 类（实体、服务、控制器、工具类）
- 3 个自定义异常类
- 2 个测试类（13 个测试用例）
- 完整的 JWT 认证系统

---

## 方法 1: 使用 IntelliJ IDEA 运行测试（推荐）

### 步骤：

1. **打开项目**
   - 在 IntelliJ IDEA 中打开 `interval-server` 目录
   - 等待 Gradle 同步完成

2. **运行单个测试类**
   - 导航到 `src/test/java/com/interval/auth/AuthServiceTest.java`
   - 右键点击类名 → 选择 "Run 'AuthServiceTest'"
   - 或者点击类名旁边的绿色运行按钮 ▶️

3. **运行所有测试**
   - 右键点击 `src/test/java` 目录
   - 选择 "Run 'All Tests'"

4. **查看测试结果**
   - 测试结果会显示在底部的 "Run" 窗口
   - 绿色 ✓ 表示通过
   - 红色 ✗ 表示失败

---

## 方法 2: 使用 VS Code 运行测试

### 前置条件：
- 安装 "Extension Pack for Java" 扩展
- 安装 "Test Runner for Java" 扩展

### 步骤：

1. **打开项目**
   - 在 VS Code 中打开 `interval-server` 目录

2. **运行测试**
   - 打开测试文件（如 `AuthServiceTest.java`）
   - 点击方法上方的 "Run Test" 或 "Debug Test" 链接
   - 或者在左侧测试面板中选择测试运行

3. **查看结果**
   - 测试结果会显示在输出面板中

---

## 方法 3: 使用命令行运行测试

### 前置条件：
需要安装 Gradle（或使用 Gradle Wrapper）

### 步骤：

1. **安装 Gradle Wrapper（如果没有）**
   ```bash
   cd d:\Project2\Interval\interval-server
   gradle wrapper
   ```

2. **运行所有测试**
   ```bash
   # Windows
   .\gradlew.bat test
   
   # 或者如果系统已安装 Gradle
   gradle test
   ```

3. **运行特定测试类**
   ```bash
   .\gradlew.bat test --tests "com.interval.auth.AuthServiceTest"
   ```

4. **查看测试报告**
   - 测试报告生成在：`build/reports/tests/test/index.html`
   - 在浏览器中打开查看详细结果

---

## 方法 4: 手动验证（无需构建工具）

如果无法运行完整测试，可以运行手动验证程序：

### 步骤：

1. **在 IDE 中打开**
   - `src/test/java/com/interval/auth/ManualVerification.java`

2. **运行 main 方法**
   - 右键点击 → "Run 'ManualVerification.main()'"
   - 或点击 main 方法旁边的绿色运行按钮

3. **查看输出**
   - 控制台会显示各项验证结果
   - ✓ 表示通过
   - ✗ 表示失败

---

## 预期测试结果

### AuthServiceTest（6 个测试）

```
✓ should_return_jwt_when_login_success
  - 验证登录成功返回 JWT token
  
✓ should_throw_exception_when_username_not_exists
  - 验证用户名不存在时抛出 AuthenticationFailedException
  
✓ should_throw_exception_when_password_incorrect
  - 验证密码错误时抛出 AuthenticationFailedException
  
✓ should_return_user_info_when_register_success
  - 验证注册成功返回用户信息
  
✓ should_throw_exception_when_username_already_exists
  - 验证用户名已存在时抛出 UserAlreadyExistsException
  
✓ should_throw_exception_when_password_too_weak
  - 验证密码强度不足时抛出 PasswordValidationException
```

### PasswordValidatorTest（7 个测试）

```
✓ should_pass_validation_for_valid_passwords (5 个参数化测试)
  - 验证有效密码通过验证
  
✓ should_throw_exception_for_short_passwords (3 个参数化测试)
  - 验证短密码抛出异常
  
✓ should_throw_exception_for_letter_only_passwords (3 个参数化测试)
  - 验证纯字母密码抛出异常
  
✓ should_throw_exception_for_number_only_passwords (3 个参数化测试)
  - 验证纯数字密码抛出异常
  
✓ should_throw_exception_for_null_password
  - 验证空密码抛出异常
  
✓ should_throw_exception_for_blank_password
  - 验证空白密码抛出异常
```

**总计**: 13 个测试用例应全部通过 ✅

---

## 常见问题排查

### 问题 1: 找不到依赖

**症状**: 编译错误，提示找不到 Spring、JWT 等类

**解决方案**:
1. 确保 `build.gradle` 中的依赖正确
2. 在 IDE 中刷新 Gradle 项目
   - IntelliJ: 右键项目 → "Reload Gradle Project"
   - VS Code: 命令面板 → "Java: Clean Java Language Server Workspace"

### 问题 2: 测试运行失败

**症状**: 测试无法启动或全部失败

**解决方案**:
1. 检查 Java 版本是否为 17
   ```bash
   java -version
   ```
2. 清理并重新构建项目
   ```bash
   gradle clean build
   ```

### 问题 3: Mock 对象错误

**症状**: 测试中 Mock 对象行为不符合预期

**解决方案**:
1. 确保使用了 `@ExtendWith(MockitoExtension.class)` 注解
2. 检查 Mock 对象的 when-then 配置是否正确

---

## 代码覆盖率检查（可选）

如果想查看测试覆盖率：

### 使用 JaCoCo

1. **添加 JaCoCo 插件到 build.gradle**:
   ```gradle
   plugins {
       id 'jacoco'
   }
   
   jacoco {
       toolVersion = "0.8.10"
   }
   
   test {
       finalizedBy jacocoTestReport
   }
   
   jacocoTestReport {
       dependsOn test
       reports {
           html.required = true
       }
   }
   ```

2. **运行测试并生成报告**:
   ```bash
   gradle test jacocoTestReport
   ```

3. **查看报告**:
   - 打开 `build/reports/jacoco/test/html/index.html`

---

## 下一步

测试通过后，可以：

1. ✅ 启动 Spring Boot 应用
   ```bash
   gradle bootRun
   ```

2. ✅ 使用 Postman 或 curl 测试 API
   ```bash
   # 注册用户
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"alex","password":"Password123"}'
   
   # 登录
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"alex","password":"Password123"}'
   ```

3. ✅ 继续实现其他功能模块（TimeSlot、Category 等）

---

## 总结

所有代码已经实现完成，符合 TDD 流程和企业级开发标准。选择上述任一方法运行测试即可验证实现的正确性。

**推荐使用 IntelliJ IDEA 或 VS Code 的图形界面运行测试，最为直观方便。**
