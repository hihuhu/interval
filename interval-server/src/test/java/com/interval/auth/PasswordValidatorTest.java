package com.interval.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordValidator 测试类
 * 
 * 测试密码验证逻辑的各种场景
 */
@DisplayName("密码验证器测试")
public class PasswordValidatorTest {
    
    private final PasswordValidator passwordValidator = new PasswordValidator();
    
    /**
     * 测试有效密码
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "Password1",
        "SecurePass123",
        "MyP@ssw0rd",
        "Test1234",
        "abcdefgh1"
    })
    @DisplayName("有效密码应通过验证")
    void should_pass_validation_for_valid_passwords(String password) {
        assertDoesNotThrow(() -> passwordValidator.validate(password),
            "有效密码应通过验证");
        assertTrue(passwordValidator.isValid(password),
            "isValid() 应返回 true");
    }
    
    /**
     * 测试无效密码 - 太短
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "Pass1",
        "abc123",
        "1234567"
    })
    @DisplayName("密码太短应抛出异常")
    void should_throw_exception_for_short_passwords(String password) {
        PasswordValidationException exception = assertThrows(
            PasswordValidationException.class,
            () -> passwordValidator.validate(password),
            "密码太短应抛出 PasswordValidationException"
        );
        
        assertTrue(exception.getMessage().contains("at least"),
            "异常消息应包含长度要求");
        assertFalse(passwordValidator.isValid(password),
            "isValid() 应返回 false");
    }
    
    /**
     * 测试无效密码 - 只有字母
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "abcdefgh",
        "Password",
        "OnlyLetters"
    })
    @DisplayName("只有字母的密码应抛出异常")
    void should_throw_exception_for_letter_only_passwords(String password) {
        PasswordValidationException exception = assertThrows(
            PasswordValidationException.class,
            () -> passwordValidator.validate(password),
            "只有字母的密码应抛出 PasswordValidationException"
        );
        
        assertTrue(exception.getMessage().contains("letters and numbers"),
            "异常消息应包含字母和数字要求");
        assertFalse(passwordValidator.isValid(password),
            "isValid() 应返回 false");
    }
    
    /**
     * 测试无效密码 - 只有数字
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "12345678",
        "98765432",
        "11111111"
    })
    @DisplayName("只有数字的密码应抛出异常")
    void should_throw_exception_for_number_only_passwords(String password) {
        PasswordValidationException exception = assertThrows(
            PasswordValidationException.class,
            () -> passwordValidator.validate(password),
            "只有数字的密码应抛出 PasswordValidationException"
        );
        
        assertTrue(exception.getMessage().contains("letters and numbers"),
            "异常消息应包含字母和数字要求");
        assertFalse(passwordValidator.isValid(password),
            "isValid() 应返回 false");
    }
    
    /**
     * 测试空密码
     */
    @Test
    @DisplayName("空密码应抛出异常")
    void should_throw_exception_for_null_password() {
        PasswordValidationException exception = assertThrows(
            PasswordValidationException.class,
            () -> passwordValidator.validate(null),
            "空密码应抛出 PasswordValidationException"
        );
        
        assertTrue(exception.getMessage().contains("empty"),
            "异常消息应包含 'empty'");
    }
    
    /**
     * 测试空白密码
     */
    @Test
    @DisplayName("空白密码应抛出异常")
    void should_throw_exception_for_blank_password() {
        PasswordValidationException exception = assertThrows(
            PasswordValidationException.class,
            () -> passwordValidator.validate("   "),
            "空白密码应抛出 PasswordValidationException"
        );
        
        assertTrue(exception.getMessage().contains("empty"),
            "异常消息应包含 'empty'");
    }
}
