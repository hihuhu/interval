package com.interval.auth;

import jakarta.persistence.*;
import lombok.Data;

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
}
