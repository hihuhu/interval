package com.interval.auth.service;

import com.interval.auth.dto.LoginResponseDto;
import com.interval.auth.dto.RegisterResponseDto;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.exception.AuthenticationFailedException;
import com.interval.auth.exception.PasswordValidationException;
import com.interval.auth.exception.UserAlreadyExistsException;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.util.JwtUtil;
import com.interval.auth.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    @Override
    @Transactional
    public LoginResponseDto login(String username, String password) {
        log.debug("Attempting login for username: {}", username);

        if (username == null || username.isBlank()) {
            log.warn("Login attempt with empty username");
            throw new AuthenticationFailedException("Invalid username or password");
        }

        if (password == null || password.isBlank()) {
            log.warn("Login attempt with empty password for username: {}", username);
            throw new AuthenticationFailedException("Invalid username or password");
        }

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("Login failed: user not found - {}", username);
                return new AuthenticationFailedException("Invalid username or password");
            });

        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn("Login failed: disabled account - {}", username);
            throw new AuthenticationFailedException("Invalid username or password");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: incorrect password for username: {}", username);
            throw new AuthenticationFailedException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", username);
        return new LoginResponseDto(
            token,
            user.getUsername(),
            user.getAccountType().name(),
            user.isMustChangePassword()
        );
    }

    @Override
    @Transactional
    public RegisterResponseDto register(String username, String password) {
        log.debug("Attempting registration for username: {}", username);

        validateUsername(username);

        if (userRepository.existsByUsername(username)) {
            log.warn("Registration failed: username already exists - {}", username);
            throw new UserAlreadyExistsException("Username already exists");
        }

        passwordValidator.validate(password);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));

        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        return new RegisterResponseDto(savedUser.getId(), savedUser.getUsername());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid username or password");
        }

        passwordValidator.validate(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setPasswordUpdatedAt(Instant.now());
        userRepository.save(user);
    }

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

        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, underscores and hyphens");
        }
    }
}
