package com.example.shorturl.common.annotation;

import java.lang.annotation.*;

/**
 * 权限控制注解
 * <p>
 * 模块职责：
 * - 定义方法级别的权限控制
 * - 支持基于角色的访问控制
 * - 支持基于权限的访问控制
 * <p>
 * 使用场景：
 * - 控制器方法权限控制
 * - 服务方法权限控制
 * - 自定义权限验证
 * <p>
 * 示例：
 * ```java
 *
 * @RequiresPermission(roles = {"ADMIN"})
 * public void adminMethod() {}
 * @RequiresPermission(permissions = {"user:create", "user:edit"})
 * public void userManagementMethod() {}
 * ```
 * <p>
 * 依赖关系：
 * - 被PermissionAspect拦截处理
 * - 与Spring Security配合使用
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 所需角色列表
     * 用户拥有任一角色即可访问
     */
    String[] roles() default {};

    /**
     * 所需权限列表
     * 用户拥有任一权限即可访问
     */
    String[] permissions() default {};

    /**
     * 访问逻辑
     * - ANY: 拥有任一角色或权限即可 (默认)
     * - ALL: 必须拥有所有角色和权限
     */
    Logical logical() default Logical.ANY;

    /**
     * 是否记录访问日志
     */
    boolean logAccess() default true;

    /**
     * 访问被拒绝时的错误消息
     */
    String message() default "权限不足，无法访问该资源";

    /**
     * 访问逻辑枚举
     */
    enum Logical {
        /**
         * 任一条件满足即可
         */
        ANY,

        /**
         * 所有条件都必须满足
         */
        ALL
    }
}