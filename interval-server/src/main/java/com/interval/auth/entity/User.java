package com.interval.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * 用户实体类
 * 
 * 存储用户的基本信息和认证凭据
 */
@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType = AccountType.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private boolean mustChangePassword = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant lastLoginAt;

    private Instant lastActiveAt;

    private Instant passwordUpdatedAt;
}
