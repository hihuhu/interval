package com.interval.auth.controller;

import com.interval.auth.dto.LoginRequest;
import com.interval.auth.dto.LoginResponseDto;
import com.interval.auth.dto.RegisterRequest;
import com.interval.auth.dto.RegisterResponseDto;
import com.interval.auth.service.AuthService;
import com.interval.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 * 提供用户登录和注册的 REST API 端点
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     * 
     * POST /api/auth/login
     * 
     * @param request 登录请求
     * @return ResponseEntity<ApiResponse<LoginResponseDto>>
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody LoginRequest request) {
        log.info("Login request received for username: {}", request.username());
        
        LoginResponseDto response = authService.login(request.username(), request.password());
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    /**
     * 用户注册
     * 
     * POST /api/auth/register
     * 
     * @param request 注册请求
     * @return ResponseEntity<ApiResponse<RegisterResponseDto>>
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(@RequestBody RegisterRequest request) {
        log.info("Register request received for username: {}", request.username());
        
        RegisterResponseDto response = authService.register(request.username(), request.password());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }
}
