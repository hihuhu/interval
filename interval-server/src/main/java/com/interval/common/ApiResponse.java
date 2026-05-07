package com.interval.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应格式
 * 
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    /**
     * 响应结果：SUCCESS 或 ERROR
     */
    private String result;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 创建成功响应
     * 
     * @param message 成功消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse<T>
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }
    
    /**
     * 创建成功响应（无消息）
     * 
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse<T>
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "Operation successful", data);
    }
    
    /**
     * 创建错误响应
     * 
     * @param message 错误消息
     * @return ApiResponse<?>
     */
    public static ApiResponse<?> error(String message) {
        return new ApiResponse<>("ERROR", message, null);
    }
}
