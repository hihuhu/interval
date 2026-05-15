package com.interval.auth;

import com.interval.auth.dto.LoginRequest;
import com.interval.auth.dto.RegisterRequest;
import com.interval.auth.exception.AuthenticationFailedException;
import com.interval.auth.exception.PasswordValidationException;
import com.interval.auth.exception.UserAlreadyExistsException;
import com.interval.auth.util.PasswordValidator;

/**
 * 手动测试验证程序
 * 
 * 用于快速验证核心功能是否正常工作
 */
public class ManualVerification {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Interval 认证模块手动验证");
        System.out.println("=".repeat(60));
        System.out.println();
        
        // 1. 测试 PasswordValidator
        testPasswordValidator();
        
        // 2. 测试 DTO 验证
        testDtoValidation();
        
        // 3. 测试异常类
        testExceptions();
        
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("手动验证完成！");
        System.out.println("=".repeat(60));
    }
    
    private static void testPasswordValidator() {
        System.out.println("1. 测试 PasswordValidator");
        System.out.println("-".repeat(60));
        
        PasswordValidator validator = new PasswordValidator();
        
        // 测试有效密码
        try {
            validator.validate("Password123");
            System.out.println("✓ 有效密码 'Password123' 通过验证");
        } catch (Exception e) {
            System.out.println("✗ 有效密码验证失败: " + e.getMessage());
        }
        
        // 测试无效密码 - 太短
        try {
            validator.validate("Pass1");
            System.out.println("✗ 短密码应该抛出异常但没有");
        } catch (PasswordValidationException e) {
            System.out.println("✓ 短密码正确抛出异常: " + e.getMessage());
        }
        
        // 测试无效密码 - 只有字母
        try {
            validator.validate("Password");
            System.out.println("✗ 纯字母密码应该抛出异常但没有");
        } catch (PasswordValidationException e) {
            System.out.println("✓ 纯字母密码正确抛出异常: " + e.getMessage());
        }
        
        // 测试无效密码 - 只有数字
        try {
            validator.validate("12345678");
            System.out.println("✗ 纯数字密码应该抛出异常但没有");
        } catch (PasswordValidationException e) {
            System.out.println("✓ 纯数字密码正确抛出异常: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testDtoValidation() {
        System.out.println("2. 测试 DTO 验证");
        System.out.println("-".repeat(60));
        
        // 测试有效的 LoginRequest
        try {
            LoginRequest request = new LoginRequest("alex", "Password123");
            System.out.println("✓ 有效的 LoginRequest 创建成功: " + request.username());
        } catch (Exception e) {
            System.out.println("✗ 有效的 LoginRequest 创建失败: " + e.getMessage());
        }
        
        // 测试无效的 LoginRequest - 空用户名
        try {
            LoginRequest request = new LoginRequest("", "Password123");
            System.out.println("✗ 空用户名应该抛出异常但没有");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ 空用户名正确抛出异常: " + e.getMessage());
        }
        
        // 测试有效的 RegisterRequest
        try {
            RegisterRequest request = new RegisterRequest("newuser", "Password123");
            System.out.println("✓ 有效的 RegisterRequest 创建成功: " + request.username());
        } catch (Exception e) {
            System.out.println("✗ 有效的 RegisterRequest 创建失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testExceptions() {
        System.out.println("3. 测试自定义异常");
        System.out.println("-".repeat(60));
        
        // 测试 AuthenticationFailedException
        try {
            throw new AuthenticationFailedException("Invalid credentials");
        } catch (AuthenticationFailedException e) {
            System.out.println("✓ AuthenticationFailedException 工作正常: " + e.getMessage());
        }
        
        // 测试 UserAlreadyExistsException
        try {
            throw new UserAlreadyExistsException("Username already exists");
        } catch (UserAlreadyExistsException e) {
            System.out.println("✓ UserAlreadyExistsException 工作正常: " + e.getMessage());
        }
        
        // 测试 PasswordValidationException
        try {
            throw new PasswordValidationException("Password too weak");
        } catch (PasswordValidationException e) {
            System.out.println("✓ PasswordValidationException 工作正常: " + e.getMessage());
        }
        
        System.out.println();
    }
}
