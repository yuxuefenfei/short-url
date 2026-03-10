package com.example.shorturl.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * <p>
 * 模块职责：
 * - 标记需要记录操作日志的方法
 * - 定义操作类型、模块和描述信息
 * - 配合AOP切面实现自动日志记录
 * <p>
 * 使用场景：
 * - 管理后台的所有操作
 * - 重要的业务操作
 * - 需要审计的用户行为
 * <p>
 * 依赖关系：
 * - 被OperationLogAspect使用
 * - 与异步日志服务配合
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresLog {

    /**
     * 操作类型
     * 如：CREATE, UPDATE, DELETE, QUERY, LOGIN, LOGOUT等
     */
    String type() default "";

    /**
     * 操作模块
     * 如：USER_MANAGEMENT, URL_MANAGEMENT, SYSTEM_CONFIG等
     */
    String module() default "";

    /**
     * 操作描述
     * 具体的操作说明，如："创建用户", "删除短网址"等
     */
    String description() default "";

    /**
     * 是否记录请求参数
     * 默认为true，对于敏感操作可设置为false
     */
    boolean logParams() default true;

    /**
     * 是否记录响应结果
     * 默认为true，对于大数据量响应可设置为false
     */
    boolean logResult() default true;

    /**
     * 日志级别
     * 默认为INFO，重要操作可设置为WARN或ERROR
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * 日志级别枚举
     */
    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}