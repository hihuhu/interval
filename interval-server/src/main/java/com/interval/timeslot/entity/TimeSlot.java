package com.interval.timeslot.entity;

import com.interval.category.entity.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 时间块实体
 */
@Entity
@Table(
    name = "time_slots",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_time_slots_user_date_slot",
            columnNames = {"user_id", "date", "slot_index"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;
    
    @Column(name = "activity_name", nullable = false)
    private String activityName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
