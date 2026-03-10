package com.example.shorturl.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * <p>
 * 模块职责：
 * - 对应数据库user表
 * - 存储用户账户信息和权限
 * - 支持用户认证和授权
 * <p>
 * 表结构说明：
 * - username: 用户名，唯一索引
 * - password: BCrypt加密存储
 * - role: 角色权限（USER/ADMIN）
 * - status: 账户状态管理
 * - last_login_time: 最后登录时间记录
 * <p>
 * 依赖关系：
 * - 被UserService和AuthService使用
 * - 与UserOperationLog关联记录操作日志
 */
@Data
@Table("user")
public class User {

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 加密密码（BCrypt）
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 角色：USER/ADMIN
     */
    private String role;

    /**
     * 状态：1正常，0禁用
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    @Column("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    @Column("created_time")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Column("updated_time")
    private LocalDateTime updatedTime;

    // ==================== 构造函数 ====================

    public User() {
        this.role = "USER";
        this.status = 1;
    }

    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }

    // ==================== 业务方法 ====================

    /**
     * 判断用户是否可用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * 判断是否为普通用户
     */
    public boolean isUser() {
        return "USER".equals(role);
    }

    /**
     * 禁用用户
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 启用用户
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLoginTime() {
        this.lastLoginTime = LocalDateTime.now();
    }

    /**
     * 获取用户显示名称
     */
    public String getDisplayName() {
        if (email != null && !email.isEmpty()) {
            return username + " (" + email + ")";
        }
        return username;
    }

    /**
     * 判断用户是否有指定角色
     */
    public boolean hasRole(String targetRole) {
        return targetRole != null && targetRole.equals(this.role);
    }

    /**
     * 判断用户是否有权限（管理员拥有所有权限）
     */
    public boolean hasPermission(String permission) {
        return isAdmin(); // 管理员拥有所有权限
        // TODO: 可以实现更细粒度的权限控制
    }
}