package com.interval.timeslot.repository;

import com.interval.timeslot.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 时间块数据访问层
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    
    /**
     * 统计某个分类被使用的次数
     */
    @Query("SELECT COUNT(ts) FROM TimeSlot ts WHERE ts.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * 检查某个分类是否被使用
     */
    @Query("SELECT CASE WHEN COUNT(ts) > 0 THEN true ELSE false END FROM TimeSlot ts WHERE ts.category.id = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);
}
