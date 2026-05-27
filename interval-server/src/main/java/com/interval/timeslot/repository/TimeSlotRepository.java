package com.interval.timeslot.repository;

import com.interval.stats.dto.CategoryDurationStatRawDto;
import com.interval.timeslot.entity.TimeSlot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 时间块数据访问层
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    
    @EntityGraph(attributePaths = {"category"})
    List<TimeSlot> findByUserIdAndDateOrderBySlotIndexAsc(Long userId, LocalDate date);

    @EntityGraph(attributePaths = {"category"})
    Optional<TimeSlot> findByUserIdAndDateAndSlotIndex(Long userId, LocalDate date, Integer slotIndex);

    @EntityGraph(attributePaths = {"category"})
    Optional<TimeSlot> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"category"})
    List<TimeSlot> findByUserIdAndIdIn(Long userId, List<Long> ids);

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

    @Query("""
        SELECT new com.interval.stats.dto.CategoryDurationStatRawDto(
            c.id,
            c.name,
            c.colorCode,
            c.status,
            c.displayOrder,
            COUNT(ts.id)
        )
        FROM TimeSlot ts
        JOIN ts.category c
        WHERE ts.userId = :userId
          AND ts.date BETWEEN :startDate AND :endDate
        GROUP BY c.id, c.name, c.colorCode, c.status, c.displayOrder
        ORDER BY COUNT(ts.id) DESC, c.displayOrder ASC, c.name ASC
    """)
    List<CategoryDurationStatRawDto> findCategoryDurationStats(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
