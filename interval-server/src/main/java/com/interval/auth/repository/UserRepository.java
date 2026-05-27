package com.interval.auth.repository;

import com.interval.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 * 
 * 提供用户的 CRUD 操作
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户对象（如果存在）
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 检查用户名是否已存在
     * 
     * @param username 用户名
     * @return true 如果用户名已存在
     */
    boolean existsByUsername(String username);
}
