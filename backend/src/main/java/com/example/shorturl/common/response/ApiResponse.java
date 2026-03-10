package com.example.shorturl.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API统一响应封装类
 * <p>
 * 模块职责：
 * - 统一所有API接口的响应格式
 * - 包含响应状态、消息、数据、时间戳和请求ID
 * - 支持泛型数据返回
 * <p>
 * 响应格式：
 * {
 * "code": 200,
 * "message": "操作成功",
 * "data": {...},
 * "timestamp": "2024-01-01T10:00:00Z",
 * "requestId": "req_abc123"
 * }
 * <p>
 * 依赖关系：
 * - 被所有Controller使用
 * - 与GlobalExceptionHandler配合处理异常响应
 */
@Data
public class ApiResponse<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳 (ISO8601格式)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    private LocalDateTime timestamp;

    /**
     * 请求ID (用于链路追踪)
     */
    private String requestId;

    private ApiResponse() {
        this.timestamp = LocalDateTime.now();
        this.requestId = generateRequestId();
    }

    private ApiResponse(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应 - 无数据
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResponseStatus.SUCCESS.getCode(), ResponseStatus.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应 - 有数据
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseStatus.SUCCESS.getCode(), ResponseStatus.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应 - 自定义消息和数据
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResponseStatus.SUCCESS.getCode(), message, data);
    }

    /**
     * 错误响应 - 使用错误码枚举
     */
    public static <T> ApiResponse<T> error(ResponseStatus status) {
        return new ApiResponse<>(status.getCode(), status.getMessage(), null);
    }

    /**
     * 错误响应 - 自定义错误码和消息
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 错误响应 - 默认系统错误
     */
    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>(ResponseStatus.INTERNAL_ERROR.getCode(),
                ResponseStatus.INTERNAL_ERROR.getMessage(), null);
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().substring(0, 8);
    }
}