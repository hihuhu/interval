package com.interval.category.entity;

/**
 * 分类状态枚举
 */
public enum CategoryStatus {
    /**
     * 激活状态：可在新增时间块时选择
     */
    ACTIVE,
    
    /**
     * 已归档：不可选择，但历史记录可见
     */
    ARCHIVED
}
