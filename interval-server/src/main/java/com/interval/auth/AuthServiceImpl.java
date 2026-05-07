package com.interval.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类
 * 
 * 实现用户登录、注册等认证相关业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    
    /**
     * 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录响应（包含 JWT token）
     * @throws AuthenticationFailedException 如果用户名或密码错误
     */
    @Override
    public LoginResponseDto login(String username, String password) {
        log.debug("Attempting login for username: {}", username);
        
        // 输入验证
        if (username == null || username.isBlank()) {
            log.warn("Login attempt with empty username");
            throw new AuthenticationFailedException("Invalid username or password");
        }
        
        if (password == null || password.isBlank()) {
            log.warn("Login attempt with empty password for username: {}", username);
            throw new AuthenticationFailedException("Invalid username or password");
        }
        
        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found - {}", username);
                    return new AuthenticationFailedException("Invalid username or password");
                });
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: incorrect password for username: {}", username);
            throw new AuthenticationFailedException("Invalid username or password");
        }
        
        // 生成 JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        log.info("User logged in successfully: {}", username);
        return new LoginResponseDto(token, user.getUsername());
    }
    
    /**
     * 用户注册
     * 
     * @param username 用户名
     * @param password 密码
     * @return 注册响应（包含用户信息）
     * @throws UserAlreadyExistsException 如果用户名已存在
     * @throws PasswordValidationException 如果密码不符合要求
     * @throws IllegalArgumentException 如果用户名格式不正确
     */
    @Override
    @Transactional
    public RegisterResponseDto register(String username, String password) {
        log.debug("Attempting registration for username: {}", username);
        
        // 验证用户名
        validateUsername(username);
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            log.warn("Registration failed: username already exists - {}", username);
            throw new UserAlreadyExistsException("Username already exists");
        }
        
        // 验证密码强度
        passwordValidator.validate(password);
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        log.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        return new RegisterResponseDto(savedUser.getId(), savedUser.getUsername());
    }
    
    /**
     * 验证用户名格式
     * 
     * @param username 用户名
     * @throws IllegalArgumentException 如果用户名格式不正确
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        
        if (username.length() > 50) {
            throw new IllegalArgumentException("Username must not exceed 50 characters");
        }
        
        // 用户名只能包含字母、数字、下划线和连字符
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, underscores and hyphens");
        }
    }
}
