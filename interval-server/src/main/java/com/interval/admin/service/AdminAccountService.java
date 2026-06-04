package com.interval.admin.service;

import com.interval.admin.dto.AdminDashboardDto;
import com.interval.admin.dto.AdminUserDetailDto;
import com.interval.admin.dto.AdminUserListItemDto;
import com.interval.admin.dto.ResetPasswordRequest;
import com.interval.admin.dto.ResetPasswordResponseDto;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.security.AuthenticatedUser;

import java.util.List;

public interface AdminAccountService {

    AdminDashboardDto getDashboard(AuthenticatedUser user);

    List<AdminUserListItemDto> listUsers(AuthenticatedUser user, String keyword, UserStatus status);

    AdminUserDetailDto getUserDetail(AuthenticatedUser user, Long userId);

    ResetPasswordResponseDto resetPassword(
        AuthenticatedUser user,
        Long userId,
        ResetPasswordRequest request,
        String ipAddress,
        String userAgent
    );

    void disableUser(AuthenticatedUser user, Long userId, String ipAddress, String userAgent);

    void enableUser(AuthenticatedUser user, Long userId, String ipAddress, String userAgent);
}
