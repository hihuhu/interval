package com.interval.auth.service;

import com.interval.auth.dto.LoginResponseDto;
import com.interval.auth.dto.RegisterResponseDto;

public interface AuthService {

    LoginResponseDto login(String username, String password);

    RegisterResponseDto register(String username, String password);

    void changePassword(Long userId, String currentPassword, String newPassword);
}
