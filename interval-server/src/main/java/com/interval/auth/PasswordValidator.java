package com.interval.auth;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 密码验证器
 * 
 * 负责验证密码强度和格式
 */
@Component
public class PasswordValidator {
    
    // 密码强度正则：至少8位，包含字母和数字
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    
    private static final int MIN_LENGTH = 8;
    
    /**
     * 验证密码强度
     * 
     * @param password 待验证的密码
     * @throws PasswordValidationException 如果密码不符合要求
     */
    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new PasswordValidationException("Password cannot be empty");
        }
        
        if (password.length() < MIN_LENGTH) {
            throw new PasswordValidationException(
                String.format("Password must be at least %d characters long", MIN_LENGTH)
            );
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new PasswordValidationException(
                "Password must contain both letters and numbers"
            );
        }
    }
    
    /**
     * 检查密码是否有效
     * 
     * @param password 待检查的密码
     * @return true 如果密码有效
     */
    public boolean isValid(String password) {
        try {
            validate(password);
            return true;
        } catch (PasswordValidationException e) {
            return false;
        }
    }
}
