package com.interval.admin.controller;

import com.interval.admin.dto.AdminDashboardDto;
import com.interval.admin.dto.AdminUserDetailDto;
import com.interval.admin.dto.AdminUserListItemDto;
import com.interval.admin.dto.ResetPasswordRequest;
import com.interval.admin.dto.ResetPasswordResponseDto;
import com.interval.admin.service.AdminAccountService;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.security.AuthenticatedUser;
import com.interval.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboard(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(adminAccountService.getDashboard(user)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserListItemDto>>> listUsers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status) {
        return ResponseEntity.ok(ApiResponse.success(adminAccountService.listUsers(user, keyword, status)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> getUserDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(adminAccountService.getUserDetail(user, userId)));
    }

    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponseDto>> resetPassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId,
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        ResetPasswordResponseDto response = adminAccountService.resetPassword(
            user,
            userId,
            request,
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/users/{userId}/disable")
    public ResponseEntity<ApiResponse<Void>> disableUser(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        adminAccountService.disableUser(user, userId, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success("User disabled successfully", null));
    }

    @PostMapping("/users/{userId}/enable")
    public ResponseEntity<ApiResponse<Void>> enableUser(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        adminAccountService.enableUser(user, userId, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success("User enabled successfully", null));
    }
}
