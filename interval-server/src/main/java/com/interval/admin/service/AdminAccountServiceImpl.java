package com.interval.admin.service;

import com.interval.admin.dto.AdminDashboardDto;
import com.interval.admin.dto.AdminUserDetailDto;
import com.interval.admin.dto.AdminUserListItemDto;
import com.interval.admin.dto.ResetPasswordRequest;
import com.interval.admin.dto.ResetPasswordResponseDto;
import com.interval.admin.entity.AdminAuditAction;
import com.interval.admin.entity.AdminAuditLog;
import com.interval.admin.repository.AdminAuditLogRepository;
import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.security.AuthenticatedUser;
import com.interval.auth.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl implements AdminAccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final UserRepository userRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    @Override
    public AdminDashboardDto getDashboard(AuthenticatedUser user) {
        requireAdmin(user);
        List<User> regularUsers = regularUsers();
        long totalUsers = regularUsers.size();
        long activeUsers = regularUsers.stream().filter(item -> item.getStatus() == UserStatus.ACTIVE).count();
        long disabledUsers = regularUsers.stream().filter(item -> item.getStatus() == UserStatus.DISABLED).count();
        long activeToday = regularUsers.stream().filter(this::isActiveToday).count();
        long activeLast7Days = regularUsers.stream().filter(this::isActiveLast7Days).count();
        return new AdminDashboardDto(totalUsers, activeUsers, disabledUsers, activeToday, activeLast7Days);
    }

    @Override
    public List<AdminUserListItemDto> listUsers(AuthenticatedUser user, String keyword, UserStatus status) {
        requireAdmin(user);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return regularUsers().stream()
            .filter(item -> normalizedKeyword.isEmpty()
                || item.getUsername().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
            .filter(item -> status == null || item.getStatus() == status)
            .sorted(Comparator.comparing(User::getUsername))
            .map(this::toListItem)
            .toList();
    }

    @Override
    public AdminUserDetailDto getUserDetail(AuthenticatedUser user, Long userId) {
        requireAdmin(user);
        return toDetail(findRegularUser(userId));
    }

    @Override
    @Transactional
    public ResetPasswordResponseDto resetPassword(
            AuthenticatedUser user,
            Long userId,
            ResetPasswordRequest request,
            String ipAddress,
            String userAgent) {
        requireAdmin(user);
        User target = findRegularUser(userId);
        String temporaryPassword = resolveTemporaryPassword(request);
        passwordValidator.validate(temporaryPassword);
        target.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        target.setMustChangePassword(true);
        target.setPasswordUpdatedAt(Instant.now());
        userRepository.save(target);
        writeAudit(user, target.getId(), AdminAuditAction.RESET_PASSWORD, ipAddress, userAgent, "{\"mode\":\"" + resetMode(request) + "\"}");
        return new ResetPasswordResponseDto(temporaryPassword, true);
    }

    @Override
    @Transactional
    public void disableUser(AuthenticatedUser user, Long userId, String ipAddress, String userAgent) {
        requireAdmin(user);
        User target = findRegularUser(userId);
        target.setStatus(UserStatus.DISABLED);
        userRepository.save(target);
        writeAudit(user, target.getId(), AdminAuditAction.DISABLE_USER, ipAddress, userAgent, null);
    }

    @Override
    @Transactional
    public void enableUser(AuthenticatedUser user, Long userId, String ipAddress, String userAgent) {
        requireAdmin(user);
        User target = findRegularUser(userId);
        target.setStatus(UserStatus.ACTIVE);
        userRepository.save(target);
        writeAudit(user, target.getId(), AdminAuditAction.ENABLE_USER, ipAddress, userAgent, null);
    }

    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || user.accountType() != AccountType.ADMIN) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    private List<User> regularUsers() {
        return userRepository.findAll().stream()
            .filter(user -> user.getAccountType() == AccountType.USER)
            .toList();
    }

    private User findRegularUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getAccountType() != AccountType.USER) {
            throw new IllegalArgumentException("Cannot manage administrator account");
        }
        return user;
    }

    private AdminUserListItemDto toListItem(User user) {
        return new AdminUserListItemDto(
            user.getId(),
            user.getUsername(),
            user.getStatus().name(),
            user.getCreatedAt(),
            user.getLastLoginAt(),
            user.getLastActiveAt(),
            user.isMustChangePassword()
        );
    }

    private AdminUserDetailDto toDetail(User user) {
        return new AdminUserDetailDto(
            user.getId(),
            user.getUsername(),
            user.getStatus().name(),
            user.getCreatedAt(),
            user.getLastLoginAt(),
            user.getLastActiveAt(),
            user.isMustChangePassword()
        );
    }

    private boolean isActiveToday(User user) {
        LocalDate today = LocalDate.now();
        return isSameDate(user.getLastLoginAt(), today) || isSameDate(user.getLastActiveAt(), today);
    }

    private boolean isActiveLast7Days(User user) {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        return isAfterOrEqual(user.getLastLoginAt(), threshold) || isAfterOrEqual(user.getLastActiveAt(), threshold);
    }

    private boolean isSameDate(Instant value, LocalDate date) {
        return value != null && value.atZone(ZoneId.systemDefault()).toLocalDate().equals(date);
    }

    private boolean isAfterOrEqual(Instant value, Instant threshold) {
        return value != null && !value.isBefore(threshold);
    }

    private String resolveTemporaryPassword(ResetPasswordRequest request) {
        if ("MANUAL".equalsIgnoreCase(resetMode(request))) {
            return request.temporaryPassword();
        }
        StringBuilder builder = new StringBuilder("Tmp-");
        for (int i = 0; i < 6; i++) {
            builder.append(TEMP_PASSWORD_ALPHABET.charAt(RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length())));
        }
        return builder.append("A1").toString();
    }

    private String resetMode(ResetPasswordRequest request) {
        if (request == null || request.mode() == null || request.mode().isBlank()) {
            return "AUTO";
        }
        return request.mode().toUpperCase(Locale.ROOT);
    }

    private void writeAudit(
            AuthenticatedUser admin,
            Long targetUserId,
            AdminAuditAction action,
            String ipAddress,
            String userAgent,
            String metadataJson) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(admin.userId());
        log.setTargetUserId(targetUserId);
        log.setAction(action);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setMetadataJson(metadataJson);
        adminAuditLogRepository.save(log);
    }
}
