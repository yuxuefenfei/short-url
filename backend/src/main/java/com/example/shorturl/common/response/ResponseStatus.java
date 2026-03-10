package com.example.shorturl.common.response;

import lombok.Getter;

/**
 * 响应状态码枚举
 * <p>
 * 模块职责：
 * - 定义所有API响应状态码和对应消息
 * - 区分系统级错误码和业务级错误码
 * <p>
 * 错误码规范：
 * - 200: 成功
 * - 400-499: HTTP标准错误码
 * - 4001-4999: 业务错误码
 * - 500-599: 服务器错误
 * <p>
 * 依赖关系：
 * - 被ApiResponse类使用
 * - 被GlobalExceptionHandler使用
 */
@Getter
public enum ResponseStatus {

    // ==================== 成功状态 ====================
    SUCCESS(200, "操作成功"),

    // ==================== HTTP标准错误码 ====================
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求频率超限"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // ==================== 业务错误码 (4001-4999) ====================
    // URL相关错误
    INVALID_URL_FORMAT(4001, "URL格式不正确"),
    URL_LENGTH_EXCEEDED(4002, "URL长度超限"),
    SHORT_URL_EXISTS(4003, "短网址已存在"),
    SHORT_URL_NOT_EXIST(4004, "短网址不存在"),
    SHORT_URL_EXPIRED(4005, "短网址已过期"),
    SHORT_URL_DISABLED(4006, "短网址已被禁用"),

    // 用户相关错误
    USER_NOT_EXIST(4007, "用户不存在"),
    PASSWORD_ERROR(4008, "密码错误"),
    USERNAME_EXISTS(4009, "用户名已存在"),
    USER_DISABLED(4010, "用户已被禁用"),

    // 系统相关错误
    SYSTEM_ERROR(5001, "系统错误，请稍后重试"),
    OPERATION_TOO_FREQUENT(4011, "操作频率超限"),
    INSUFFICIENT_PERMISSION(4012, "权限不足"),
    DATA_INTEGRITY_ERROR(4013, "数据完整性错误"),
    CACHE_ERROR(4014, "缓存操作失败"),
    DATABASE_ERROR(4015, "数据库操作失败"),

    // 文件相关错误
    FILE_UPLOAD_ERROR(4020, "文件上传失败"),
    FILE_SIZE_EXCEEDED(4021, "文件大小超限"),
    UNSUPPORTED_FILE_TYPE(4022, "不支持的文件类型"),

    // 验证码相关错误
    CAPTCHA_ERROR(4030, "验证码错误"),
    CAPTCHA_EXPIRED(4031, "验证码已过期"),
    SMS_CODE_ERROR(4032, "短信验证码错误"),
    SMS_CODE_EXPIRED(4033, "短信验证码已过期"),

    // 限流相关错误
    RATE_LIMIT_EXCEEDED(4040, "访问频率超限，请稍后重试"),
    IP_BLACKLISTED(4041, "IP已被加入黑名单"),

    // 第三方服务错误
    THIRD_PARTY_SERVICE_ERROR(4050, "第三方服务调用失败"),
    PAYMENT_ERROR(4051, "支付处理失败"),
    EMAIL_SEND_ERROR(4052, "邮件发送失败"),
    SMS_SEND_ERROR(4053, "短信发送失败");

    private final int code;
    private final String message;

    ResponseStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据错误码获取枚举
     */
    public static ResponseStatus getByCode(int code) {
        for (ResponseStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return INTERNAL_ERROR;
    }

}