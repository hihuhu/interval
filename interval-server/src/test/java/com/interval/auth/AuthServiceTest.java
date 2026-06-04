package com.interval.auth;

import com.interval.auth.dto.LoginResponseDto;
import com.interval.auth.dto.RegisterResponseDto;
import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.exception.AuthenticationFailedException;
import com.interval.auth.exception.PasswordValidationException;
import com.interval.auth.exception.UserAlreadyExistsException;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.service.AuthServiceImpl;
import com.interval.auth.util.JwtUtil;
import com.interval.auth.util.PasswordValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService 测试类
 * 
 * 测试认证模块的核心业务逻辑，包括：
 * - 用户登录（成功、失败场景）
 * - 用户注册（成功、失败场景）
 * - JWT token 生成与验证
 * 
 * 测试策略：
 * - 使用 Mockito 模拟 UserRepository 和 JwtUtil
 * - 验证 Service 层的业务逻辑，不涉及真实数据库
 * - 覆盖正常流程和异常场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private String testUsername = "alex";
    private String testPassword = "SecurePass123!";
    private String testPasswordHash = "$2a$12$hashedPasswordExample";
    private String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example.token";

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(testUsername);
        testUser.setPasswordHash(testPasswordHash);
        testUser.setAccountType(AccountType.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setMustChangePassword(false);
    }

    /**
     * 测试用例 1：登录成功返回 JWT
     * 
     * Given: 数据库中存在用户 alex，密码哈希为 BCrypt 加密的 SecurePass123!
     * When: 调用 AuthService.login("alex", "SecurePass123!")
     * Then: 
     *   - 返回 LoginResponseDto
     *   - token 字段非空
     *   - token 格式符合 JWT 规范（三段式，用 . 分隔）
     *   - username 字段为 "alex"
     *   - 不抛出任何异常
     */
    @Test
    @DisplayName("登录成功应返回有效的 JWT token")
    void should_return_jwt_when_login_success() {
        // Given: 用户存在且密码正确
        when(userRepository.findByUsername(testUsername))
            .thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, testPasswordHash))
            .thenReturn(true);
        when(jwtUtil.generateToken(testUser.getId(), testUsername))
            .thenReturn(testToken);

        // When: 调用登录方法
        LoginResponseDto response = authService.login(testUsername, testPassword);

        // Then: 验证返回结果
        assertNotNull(response, "登录响应不应为空");
        assertNotNull(response.token(), "JWT token 不应为空");
        assertEquals(testToken, response.token(), "返回的 token 应与生成的 token 一致");
        assertEquals(testUsername, response.username(), "返回的用户名应正确");
        assertEquals("USER", response.accountType(), "登录响应应返回账号类型");
        assertFalse(response.mustChangePassword(), "登录响应应返回是否需要强制改密");
        
        // 验证 JWT token 格式（三段式，用 . 分隔）
        String[] tokenParts = response.token().split("\\.");
        assertEquals(3, tokenParts.length, "JWT token 应为三段式结构");
        
        // 验证方法调用
        verify(userRepository, times(1)).findByUsername(testUsername);
        verify(passwordEncoder, times(1)).matches(testPassword, testPasswordHash);
        verify(jwtUtil, times(1)).generateToken(testUser.getId(), testUsername);
        verify(userRepository, times(1)).save(argThat(user ->
            user.getId().equals(testUser.getId()) && user.getLastLoginAt() != null
        ));
    }

    @Test
    @DisplayName("禁用账号登录应失败")
    void should_throw_exception_when_account_disabled() {
        testUser.setStatus(UserStatus.DISABLED);
        when(userRepository.findByUsername(testUsername))
            .thenReturn(java.util.Optional.of(testUser));

        AuthenticationFailedException exception = assertThrows(
            AuthenticationFailedException.class,
            () -> authService.login(testUsername, testPassword),
            "禁用账号登录时应抛出 AuthenticationFailedException"
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("登录成功应更新最近登录时间")
    void should_update_last_login_time_when_login_success() {
        assertNull(testUser.getLastLoginAt());
        when(userRepository.findByUsername(testUsername))
            .thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, testPasswordHash))
            .thenReturn(true);
        when(jwtUtil.generateToken(testUser.getId(), testUsername))
            .thenReturn(testToken);

        Instant beforeLogin = Instant.now();

        authService.login(testUsername, testPassword);

        verify(userRepository).save(argThat(user ->
            user.getLastLoginAt() != null && !user.getLastLoginAt().isBefore(beforeLogin)
        ));
    }

    /**
     * 测试用例 2：用户名不存在
     * 
     * Given: 数据库中不存在用户 nonexistent
     * When: 调用 AuthService.login("nonexistent", "anyPassword")
     * Then:
     *   - 抛出 AuthenticationFailedException
     *   - 异常消息为 "Invalid username or password"
     */
    @Test
    @DisplayName("用户名不存在时应抛出异常")
    void should_throw_exception_when_username_not_exists() {
        // Given: 用户不存在
        String nonexistentUsername = "nonexistent";
        when(userRepository.findByUsername(nonexistentUsername))
            .thenReturn(java.util.Optional.empty());

        // When & Then: 调用登录方法应抛出异常
        AuthenticationFailedException exception = assertThrows(
            AuthenticationFailedException.class,
            () -> authService.login(nonexistentUsername, "anyPassword"),
            "用户名不存在时应抛出 AuthenticationFailedException"
        );

        assertEquals("Invalid username or password", exception.getMessage(),
            "异常消息应为 'Invalid username or password'");

        // 验证方法调用
        verify(userRepository, times(1)).findByUsername(nonexistentUsername);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    /**
     * 测试用例 3：密码错误
     * 
     * Given: 数据库中存在用户 alex，密码哈希为 BCrypt 加密的 SecurePass123!
     * When: 调用 AuthService.login("alex", "WrongPassword")
     * Then:
     *   - 抛出 AuthenticationFailedException
     *   - 异常消息为 "Invalid username or password"
     */
    @Test
    @DisplayName("密码错误时应抛出异常")
    void should_throw_exception_when_password_incorrect() {
        // Given: 用户存在但密码错误
        String wrongPassword = "WrongPassword";
        when(userRepository.findByUsername(testUsername))
            .thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testPasswordHash))
            .thenReturn(false);

        // When & Then: 调用登录方法应抛出异常
        AuthenticationFailedException exception = assertThrows(
            AuthenticationFailedException.class,
            () -> authService.login(testUsername, wrongPassword),
            "密码错误时应抛出 AuthenticationFailedException"
        );

        assertEquals("Invalid username or password", exception.getMessage(),
            "异常消息应为 'Invalid username or password'");

        // 验证方法调用
        verify(userRepository, times(1)).findByUsername(testUsername);
        verify(passwordEncoder, times(1)).matches(wrongPassword, testPasswordHash);
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    /**
     * 测试用例 4：注册成功
     * 
     * Given: 数据库中不存在用户 newuser
     * When: 调用 AuthService.register("newuser", "SecurePass123!")
     * Then:
     *   - 返回 RegisterResponseDto
     *   - userId 字段非空
     *   - username 字段为 "newuser"
     *   - 数据库中新增一条用户记录
     *   - 密码字段存储的是 BCrypt 哈希值，不是明文
     */
    @Test
    @DisplayName("注册成功应返回用户信息")
    void should_return_user_info_when_register_success() {
        // Given: 用户名不存在
        String newUsername = "newuser";
        String newPassword = "SecurePass123!";
        String hashedPassword = "$2a$12$newHashedPassword";
        
        when(userRepository.existsByUsername(newUsername))
            .thenReturn(false);
        doNothing().when(passwordValidator).validate(newPassword);
        when(passwordEncoder.encode(newPassword))
            .thenReturn(hashedPassword);
        
        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername(newUsername);
        savedUser.setPasswordHash(hashedPassword);
        
        when(userRepository.save(any(User.class)))
            .thenReturn(savedUser);

        // When: 调用注册方法
        RegisterResponseDto response = authService.register(newUsername, newPassword);

        // Then: 验证返回结果
        assertNotNull(response, "注册响应不应为空");
        assertNotNull(response.userId(), "用户 ID 不应为空");
        assertEquals(2L, response.userId(), "用户 ID 应正确");
        assertEquals(newUsername, response.username(), "用户名应正确");

        // 验证方法调用
        verify(userRepository, times(1)).existsByUsername(newUsername);
        verify(passwordValidator, times(1)).validate(newPassword);
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(userRepository, times(1)).save(argThat(user -> 
            user.getUsername().equals(newUsername) && 
            user.getPasswordHash().equals(hashedPassword)
        ));
    }

    /**
     * 测试用例 5：注册时用户名已存在
     * 
     * Given: 数据库中已存在用户 alex
     * When: 调用 AuthService.register("alex", "AnyPassword123!")
     * Then:
     *   - 抛出 UserAlreadyExistsException
     *   - 异常消息为 "Username already exists"
     */
    @Test
    @DisplayName("用户名已存在时注册应抛出异常")
    void should_throw_exception_when_username_already_exists() {
        // Given: 用户名已存在
        when(userRepository.existsByUsername(testUsername))
            .thenReturn(true);

        // When & Then: 调用注册方法应抛出异常
        UserAlreadyExistsException exception = assertThrows(
            UserAlreadyExistsException.class,
            () -> authService.register(testUsername, "AnyPassword123!"),
            "用户名已存在时应抛出 UserAlreadyExistsException"
        );

        assertEquals("Username already exists", exception.getMessage(),
            "异常消息应为 'Username already exists'");

        // 验证方法调用
        verify(userRepository, times(1)).existsByUsername(testUsername);
        verify(passwordValidator, never()).validate(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    /**
     * 测试用例 6：密码强度验证
     * 
     * Given: 用户尝试使用弱密码注册
     * When: 调用 AuthService.register("newuser", "123")
     * Then:
     *   - 抛出 PasswordValidationException
     *   - 异常消息包含密码强度要求
     */
    @Test
    @DisplayName("密码强度不足时注册应抛出异常")
    void should_throw_exception_when_password_too_weak() {
        // Given: 弱密码
        String weakPassword = "123";
        String newUsername = "newuser";
        
        when(userRepository.existsByUsername(newUsername))
            .thenReturn(false);
        doThrow(new PasswordValidationException("Password must be at least 8 characters long"))
            .when(passwordValidator).validate(weakPassword);

        // When & Then: 调用注册方法应抛出异常
        PasswordValidationException exception = assertThrows(
            PasswordValidationException.class,
            () -> authService.register(newUsername, weakPassword),
            "密码强度不足时应抛出 PasswordValidationException"
        );

        assertTrue(exception.getMessage().contains("Password"),
            "异常消息应包含密码相关提示");

        // 验证不应调用数据库操作
        verify(passwordValidator, times(1)).validate(weakPassword);
        verify(userRepository, never()).save(any());
    }
}
