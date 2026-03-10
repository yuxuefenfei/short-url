package com.example.shorturl.common.exception;

import com.example.shorturl.common.response.ResponseStatus;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常类
 * <p>
 * 模块职责：
 * - 封装业务逻辑相关的异常
 * - 提供错误码和错误消息
 * - 支持异常链传递
 * <p>
 * 使用场景：
 * - 参数验证失败
 * - 业务规则校验失败
 * - 数据状态异常
 * - 权限验证失败
 * <p>
 * 依赖关系：
 * - 被Service层抛出
 * - 被GlobalExceptionHandler捕获处理
 */
public class BusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    @Getter
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造函数 - 使用ResponseStatus枚举
     */
    public BusinessException(ResponseStatus status) {
        super(status.getMessage());
        this.code = status.getCode();
        this.message = status.getMessage();
    }

    /**
     * 构造函数 - 自定义错误码和消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数 - 带原因异常
     */
    public BusinessException(ResponseStatus status, Throwable cause) {
        super(status.getMessage(), cause);
        this.code = status.getCode();
        this.message = status.getMessage();
    }

    /**
     * 构造函数 - 自定义错误码、消息和原因异常
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 判断是否为可恢复异常
     */
    public boolean isRecoverable() {
        // 客户端错误通常可恢复，服务器错误可能需要人工干预
        return code >= 4000 && code < 5000;
    }

    /**
     * 获取异常类型描述
     */
    public String getExceptionType() {
        if (code >= 4001 && code <= 4006) {
            return "URL_ERROR";
        } else if (code >= 4007 && code <= 4010) {
            return "USER_ERROR";
        } else if (code >= 4011 && code <= 4015) {
            return "SYSTEM_ERROR";
        } else if (code >= 4020 && code <= 4022) {
            return "FILE_ERROR";
        } else if (code >= 4030 && code <= 4033) {
            return "CAPTCHA_ERROR";
        } else if (code >= 4040 && code <= 4041) {
            return "RATE_LIMIT_ERROR";
        } else if (code >= 4050 && code <= 4053) {
            return "THIRD_PARTY_ERROR";
        } else {
            return "BUSINESS_ERROR";
        }
    }
}