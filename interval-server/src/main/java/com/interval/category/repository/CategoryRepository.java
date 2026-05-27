package com.interval.category.repository;

import com.interval.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 分类数据访问层
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * 查询用户的所有活跃分类
     */
    @Query("SELECT c FROM Category c WHERE c.userId = :userId AND c.status = 'ACTIVE' ORDER BY c.displayOrder")
    List<Category> findActiveByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户的某个分类（用于权限校验）
     */
    @Query("SELECT c FROM Category c WHERE c.userId = :userId AND c.id = :id")
    Optional<Category> findByUserIdAndId(@Param("userId") Long userId, @Param("id") Long id);
    
    /**
     * 检查用户是否已有同名的活跃分类
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE c.userId = :userId AND c.name = :name AND c.status = 'ACTIVE'")
    boolean existsActiveByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
    
    /**
     * 获取用户当前最大的 displayOrder（用于新建分类时自动排序）
     */
    @Query("SELECT COALESCE(MAX(c.displayOrder), -1) FROM Category c WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    Integer findMaxDisplayOrderByUserId(@Param("userId") Long userId);
}
