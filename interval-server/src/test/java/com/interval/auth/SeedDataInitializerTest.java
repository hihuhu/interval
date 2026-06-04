package com.interval.auth;

import com.interval.auth.config.SeedDataInitializer;
import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.repository.UserRepository;
import com.interval.category.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("默认管理员初始化测试")
class SeedDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("不存在管理员时应创建 admin/123456 且不创建默认分类")
    void should_create_default_admin_without_categories_when_admin_missing() {
        when(userRepository.findByUsername(SeedDataInitializer.ADMIN_USERNAME))
            .thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456"))
            .thenReturn("encoded-admin-password");
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });

        SeedDataInitializer initializer = new SeedDataInitializer(
            userRepository,
            categoryRepository,
            passwordEncoder
        );

        initializer.seedAdminAccount();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("admin", saved.getUsername());
        assertEquals("encoded-admin-password", saved.getPasswordHash());
        assertEquals(AccountType.ADMIN, saved.getAccountType());
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertTrue(saved.isMustChangePassword());
        verify(passwordEncoder).encode("123456");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("已存在管理员时不应覆盖密码")
    void should_not_overwrite_password_when_admin_exists() {
        User existingAdmin = new User();
        existingAdmin.setId(1L);
        existingAdmin.setUsername(SeedDataInitializer.ADMIN_USERNAME);
        existingAdmin.setPasswordHash("existing-password-hash");
        existingAdmin.setAccountType(AccountType.ADMIN);
        existingAdmin.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByUsername(SeedDataInitializer.ADMIN_USERNAME))
            .thenReturn(Optional.of(existingAdmin));

        SeedDataInitializer initializer = new SeedDataInitializer(
            userRepository,
            categoryRepository,
            passwordEncoder
        );

        initializer.seedAdminAccount();

        verify(passwordEncoder, never()).encode("123456");
        verify(userRepository, never()).save(any(User.class));
        verify(categoryRepository, never()).save(any());
    }
}
