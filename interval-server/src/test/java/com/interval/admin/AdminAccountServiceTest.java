package com.interval.admin;

import com.interval.admin.dto.AdminDashboardDto;
import com.interval.admin.dto.AdminUserListItemDto;
import com.interval.admin.dto.ResetPasswordRequest;
import com.interval.admin.dto.ResetPasswordResponseDto;
import com.interval.admin.entity.AdminAuditAction;
import com.interval.admin.entity.AdminAuditLog;
import com.interval.admin.repository.AdminAuditLogRepository;
import com.interval.admin.service.AdminAccountServiceImpl;
import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.security.AuthenticatedUser;
import com.interval.auth.util.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("管理员账号服务测试")
class AdminAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    private AdminAccountServiceImpl adminAccountService;
    private AuthenticatedUser adminPrincipal;

    @BeforeEach
    void setUp() {
        adminAccountService = new AdminAccountServiceImpl(
            userRepository,
            adminAuditLogRepository,
            passwordEncoder,
            passwordValidator
        );
        adminPrincipal = new AuthenticatedUser(1L, "admin", AccountType.ADMIN, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("应保存管理员账号操作审计日志实体字段")
    void should_create_admin_audit_log_entity() {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(1L);
        log.setTargetUserId(2L);
        log.setAction(AdminAuditAction.RESET_PASSWORD);
        log.setIpAddress("127.0.0.1");
        log.setUserAgent("JUnit");
        log.setMetadataJson("{\"mode\":\"AUTO\"}");

        assertThat(log.getAdminUserId()).isEqualTo(1L);
        assertThat(log.getTargetUserId()).isEqualTo(2L);
        assertThat(log.getAction()).isEqualTo(AdminAuditAction.RESET_PASSWORD);
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(log.getUserAgent()).isEqualTo("JUnit");
        assertThat(log.getMetadataJson()).isEqualTo("{\"mode\":\"AUTO\"}");
    }

    @Test
    @DisplayName("审计动作应覆盖重置密码、禁用、启用和管理员改密")
    void should_define_required_admin_audit_actions() {
        assertThat(AdminAuditAction.values())
            .contains(
                AdminAuditAction.RESET_PASSWORD,
                AdminAuditAction.DISABLE_USER,
                AdminAuditAction.ENABLE_USER,
                AdminAuditAction.CHANGE_OWN_PASSWORD
            );
    }

    @Test
    @DisplayName("dashboard 应只统计普通用户并计算活跃指标")
    void should_build_dashboard_for_regular_users_only() {
        Instant now = Instant.now();
        User activeToday = user(2L, "alex", AccountType.USER, UserStatus.ACTIVE);
        activeToday.setLastLoginAt(now.minus(2, ChronoUnit.HOURS));
        User activeLast7Days = user(3L, "beth", AccountType.USER, UserStatus.ACTIVE);
        activeLast7Days.setLastActiveAt(now.minus(3, ChronoUnit.DAYS));
        User disabled = user(4L, "carl", AccountType.USER, UserStatus.DISABLED);
        User admin = user(1L, "admin", AccountType.ADMIN, UserStatus.ACTIVE);

        when(userRepository.findAll()).thenReturn(List.of(activeToday, activeLast7Days, disabled, admin));

        AdminDashboardDto dashboard = adminAccountService.getDashboard(adminPrincipal);

        assertThat(dashboard.totalUsers()).isEqualTo(3);
        assertThat(dashboard.activeUsers()).isEqualTo(2);
        assertThat(dashboard.disabledUsers()).isEqualTo(1);
        assertThat(dashboard.activeToday()).isEqualTo(1);
        assertThat(dashboard.activeLast7Days()).isEqualTo(2);
    }

    @Test
    @DisplayName("用户列表应排除管理员并支持关键字和状态过滤")
    void should_list_regular_users_with_keyword_and_status_filter() {
        User alex = user(2L, "alex", AccountType.USER, UserStatus.ACTIVE);
        User disabledAlex = user(3L, "alex-disabled", AccountType.USER, UserStatus.DISABLED);
        User beth = user(4L, "beth", AccountType.USER, UserStatus.ACTIVE);
        User admin = user(1L, "admin", AccountType.ADMIN, UserStatus.ACTIVE);
        when(userRepository.findAll()).thenReturn(List.of(alex, disabledAlex, beth, admin));

        List<AdminUserListItemDto> users = adminAccountService.listUsers(
            adminPrincipal,
            "alex",
            UserStatus.ACTIVE
        );

        assertThat(users).hasSize(1);
        assertThat(users.get(0).username()).isEqualTo("alex");
        assertThat(users.get(0).status()).isEqualTo(UserStatus.ACTIVE.name());
    }

    @Test
    @DisplayName("重置密码应保存 hash、强制下次改密并写入不含明文的审计日志")
    void should_reset_password_and_write_audit_without_plaintext_password() {
        User target = user(2L, "alex", AccountType.USER, UserStatus.ACTIVE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        doNothing().when(passwordValidator).validate("TempPass123");
        when(passwordEncoder.encode("TempPass123")).thenReturn("hashed-temp");

        ResetPasswordResponseDto response = adminAccountService.resetPassword(
            adminPrincipal,
            2L,
            new ResetPasswordRequest("MANUAL", "TempPass123"),
            "127.0.0.1",
            "JUnit"
        );

        assertThat(response.temporaryPassword()).isEqualTo("TempPass123");
        assertThat(response.mustChangePassword()).isTrue();
        verify(userRepository).save(argThat(user ->
            user.getPasswordHash().equals("hashed-temp") && user.isMustChangePassword()
        ));
        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        AdminAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAction()).isEqualTo(AdminAuditAction.RESET_PASSWORD);
        assertThat(auditLog.getAdminUserId()).isEqualTo(1L);
        assertThat(auditLog.getTargetUserId()).isEqualTo(2L);
        assertThat(auditLog.getMetadataJson()).doesNotContain("TempPass123");
    }

    @Test
    @DisplayName("自动重置密码应生成有效临时密码且审计不包含明文")
    void should_generate_valid_temporary_password_for_auto_reset_without_audit_plaintext() {
        User target = user(2L, "alex", AccountType.USER, UserStatus.ACTIVE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-auto-temp");

        ResetPasswordResponseDto response = adminAccountService.resetPassword(
            adminPrincipal,
            2L,
            new ResetPasswordRequest("AUTO", null),
            "127.0.0.1",
            "JUnit"
        );

        assertThat(response.temporaryPassword()).startsWith("Tmp-").endsWith("A1");
        assertThat(response.temporaryPassword()).hasSize(12);
        verify(passwordValidator).validate(response.temporaryPassword());
        verify(passwordEncoder).encode(response.temporaryPassword());
        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getMetadataJson()).isEqualTo("{\"mode\":\"AUTO\"}");
        assertThat(auditCaptor.getValue().getMetadataJson()).doesNotContain(response.temporaryPassword());
    }

    @Test
    @DisplayName("禁用和启用普通用户应更新状态并写审计日志")
    void should_disable_and_enable_regular_user_with_audit_logs() {
        User target = user(2L, "alex", AccountType.USER, UserStatus.ACTIVE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        adminAccountService.disableUser(adminPrincipal, 2L, "127.0.0.1", "JUnit");
        assertThat(target.getStatus()).isEqualTo(UserStatus.DISABLED);

        adminAccountService.enableUser(adminPrincipal, 2L, "127.0.0.1", "JUnit");
        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(adminAuditLogRepository).save(argThat(log ->
            log.getAction() == AdminAuditAction.DISABLE_USER && log.getTargetUserId().equals(2L)
        ));
        verify(adminAuditLogRepository).save(argThat(log ->
            log.getAction() == AdminAuditAction.ENABLE_USER && log.getTargetUserId().equals(2L)
        ));
    }

    @Test
    @DisplayName("普通用户不能调用管理员服务")
    void should_reject_non_admin_principal() {
        AuthenticatedUser regularUser = new AuthenticatedUser(2L, "alex", AccountType.USER, UserStatus.ACTIVE);

        assertThrows(
            AccessDeniedException.class,
            () -> adminAccountService.getDashboard(regularUser)
        );
    }

    @Test
    @DisplayName("不能禁用管理员账号")
    void should_reject_managing_admin_account() {
        User targetAdmin = user(5L, "other-admin", AccountType.ADMIN, UserStatus.ACTIVE);
        when(userRepository.findById(5L)).thenReturn(Optional.of(targetAdmin));

        assertThrows(
            IllegalArgumentException.class,
            () -> adminAccountService.disableUser(adminPrincipal, 5L, "127.0.0.1", "JUnit")
        );
    }

    private User user(Long id, String username, AccountType accountType, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("hash-" + id);
        user.setAccountType(accountType);
        user.setStatus(status);
        user.setCreatedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        return user;
    }
}
