package com.interval.auth.security;

import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.UserStatus;

public record AuthenticatedUser(
    Long userId,
    String username,
    AccountType accountType,
    UserStatus status
) {
}
