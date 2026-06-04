package com.interval.auth.controller;

import com.interval.auth.dto.ChangePasswordRequest;
import com.interval.auth.dto.LoginRequest;
import com.interval.auth.dto.LoginResponseDto;
import com.interval.auth.dto.RegisterRequest;
import com.interval.auth.dto.RegisterResponseDto;
import com.interval.auth.security.AuthenticatedUser;
import com.interval.auth.service.AuthService;
import com.interval.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody LoginRequest request) {
        log.info("Login request received for username: {}", request.username());

        LoginResponseDto response = authService.login(request.username(), request.password());

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(@RequestBody RegisterRequest request) {
        log.info("Register request received for username: {}", request.username());

        RegisterResponseDto response = authService.register(request.username(), request.password());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.userId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
