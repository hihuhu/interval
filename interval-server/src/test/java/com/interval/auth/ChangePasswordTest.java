package com.interval.auth;

import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.exception.AuthenticationFailedException;
import com.interval.auth.exception.PasswordValidationException;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.service.AuthServiceImpl;
import com.interval.auth.util.JwtUtil;
import com.interval.auth.util.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("修改密码测试")
class ChangePasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    private AuthServiceImpl authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, jwtUtil, passwordEncoder, passwordValidator);
        user = new User();
        user.setId(7L);
        user.setUsername("alex");
        user.setPasswordHash("old-hash");
        user.setAccountType(AccountType.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
    }

    @Test
    @DisplayName("当前密码错误时修改密码失败")
    void should_fail_when_current_password_incorrect() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        AuthenticationFailedException exception = assertThrows(
            AuthenticationFailedException.class,
            () -> authService.changePassword(7L, "wrong-password", "NewPassword123")
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(passwordValidator, never()).validate(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("新密码强度不足时修改密码失败")
    void should_fail_when_new_password_invalid() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123", "old-hash")).thenReturn(true);
        doThrow(new PasswordValidationException("Password must contain both letters and numbers"))
            .when(passwordValidator).validate("12345678");

        assertThrows(
            PasswordValidationException.class,
            () -> authService.changePassword(7L, "OldPassword123", "12345678")
        );

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码成功后应更新 hash、清除强制改密并记录更新时间")
    void should_update_password_and_clear_must_change_password_when_success() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123", "old-hash")).thenReturn(true);
        doNothing().when(passwordValidator).validate("NewPassword123");
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hash");

        Instant beforeChange = Instant.now();

        authService.changePassword(7L, "OldPassword123", "NewPassword123");

        verify(userRepository).save(argThat(saved ->
            saved.getPasswordHash().equals("new-hash")
                && !saved.isMustChangePassword()
                && saved.getPasswordUpdatedAt() != null
                && !saved.getPasswordUpdatedAt().isBefore(beforeChange)
        ));
    }
}
