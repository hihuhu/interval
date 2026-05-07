package com.interval.common;

import com.interval.auth.AuthenticationFailedException;
import com.interval.auth.PasswordValidationException;
import com.interval.auth.UserAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 
 * 统一处理应用中的异常，返回标准的 ApiResponse 格式
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 创建错误响应实体
     * 
     * @param message 错误消息
     * @param status HTTP 状态码
     * @return ResponseEntity<ApiResponse<?>>
     */
    public static ResponseEntity<ApiResponse<?>> errorResponseEntity(String message, HttpStatus status) {
        ApiResponse<?> response = ApiResponse.error(message);
        return new ResponseEntity<>(response, status);
    }

    /**
     * 处理认证失败异常
     * 
     * @param ex 异常对象
     * @return 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationFailedException(AuthenticationFailedException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return errorResponseEntity(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * 处理用户已存在异常
     * 
     * @param ex 异常对象
     * @return 409 Conflict
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<?>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return errorResponseEntity(ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * 处理密码验证异常
     * 
     * @param ex 异常对象
     * @return 400 Bad Request
     */
    @ExceptionHandler(PasswordValidationException.class)
    public ResponseEntity<ApiResponse<?>> handlePasswordValidationException(PasswordValidationException ex) {
        log.warn("Password validation failed: {}", ex.getMessage());
        return errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理非法参数异常
     * 
     * @param ex 异常对象
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理通用异常
     * 
     * @param ex 异常对象
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return errorResponseEntity("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
