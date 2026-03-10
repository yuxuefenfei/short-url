-- 短网址管理系统数据库建表脚本
-- MySQL 8.x
-- 创建时间：2026-03-02

-- 创建数据库
CREATE DATABASE IF NOT EXISTS short_url_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE short_url_db;

-- =============================================
-- 1. 短网址映射表 (short_url_mapping)
-- =============================================
CREATE TABLE `short_url_mapping` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `short_key` VARCHAR(20) NOT NULL COMMENT '短网址key',
    `original_url` VARCHAR(2048) NOT NULL COMMENT '原始长网址',
    `title` VARCHAR(255) NULL COMMENT '网址标题',
    `click_count` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点击次数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1正常/0禁用)',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `expired_time` DATETIME NULL COMMENT '过期时间，NULL表示永不过期',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_key` (`short_key`),
    KEY `idx_created_time` (`created_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短网址映射表';

-- =============================================
-- 2. 访问日志表 (url_access_log)
-- =============================================
CREATE TABLE `url_access_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `short_key` VARCHAR(20) NOT NULL COMMENT '短网址key',
    `user_agent` TEXT NULL COMMENT '用户浏览器信息',
    `ip_address` VARCHAR(45) NULL COMMENT '访问者IP',
    `access_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    PRIMARY KEY (`id`),
    KEY `idx_short_key` (`short_key`),
    KEY `idx_access_time` (`access_time`),
    KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问日志表';

-- =============================================
-- 3. 用户表 (user)
-- =============================================
CREATE TABLE `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
    `email` VARCHAR(100) NULL COMMENT '邮箱',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色(USER/ADMIN)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1正常/0禁用)',
    `last_login_time` DATETIME NULL COMMENT '最后登录时间',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 4. 用户操作日志表 (user_operation_log)
-- =============================================
CREATE TABLE `user_operation_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `operation_desc` VARCHAR(500) NULL COMMENT '操作描述',
    `module` VARCHAR(50) NOT NULL COMMENT '操作模块',
    `ip_address` VARCHAR(45) NULL COMMENT '操作IP地址',
    `user_agent` TEXT NULL COMMENT '用户浏览器信息',
    `request_path` VARCHAR(500) NULL COMMENT '请求路径',
    `request_method` VARCHAR(10) NULL COMMENT '请求方法',
    `request_params` TEXT NULL COMMENT '请求参数',
    `response_result` TEXT NULL COMMENT '响应结果',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '操作状态(1成功/0失败)',
    `error_message` VARCHAR(1000) NULL COMMENT '错误信息',
    `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_operation_time` (`operation_time`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_module` (`module`),
    CONSTRAINT `fk_user_operation_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户操作日志表';

-- =============================================
-- 5. 初始数据
-- =============================================

-- 创建默认管理员用户（密码：admin123）
INSERT INTO `user` (`username`, `password`, `email`, `role`, `status`) VALUES
('admin', '$2a$12$VkcbkzVi1EepDMsQwOxBm.8dAn2/zRwJYDJY0LarGn.cbSPrXXbBK', 'admin@example.com', 'ADMIN', 1);

-- =============================================
-- 6. 索引优化说明
-- =============================================

-- 为提高查询性能，建议定期执行的优化操作：

-- 1. 访问日志表按月分区（生产环境建议）
-- ALTER TABLE url_access_log
-- PARTITION BY RANGE (UNIX_TIMESTAMP(access_time)) (
--     PARTITION p202401 VALUES LESS THAN (UNIX_TIMESTAMP('2024-02-01')),
--     PARTITION p202402 VALUES LESS THAN (UNIX_TIMESTAMP('2024-03-01')),
--     PARTITION p202403 VALUES LESS THAN (UNIX_TIMESTAMP('2024-04-01'))
-- );

-- 2. 操作日志表按月分区（生产环境建议）
-- ALTER TABLE user_operation_log
-- PARTITION BY RANGE (UNIX_TIMESTAMP(operation_time)) (
--     PARTITION p202401 VALUES LESS THAN (UNIX_TIMESTAMP('2024-02-01')),
--     PARTITION p202402 VALUES LESS THAN (UNIX_TIMESTAMP('2024-03-01')),
--     PARTITION p202403 VALUES LESS THAN (UNIX_TIMESTAMP('2024-04-01'))
-- );

-- 3. 定期归档3个月前的日志数据
-- CREATE EVENT archive_logs
-- ON SCHEDULE EVERY 1 MONTH
-- DO
-- BEGIN
--     -- 归档访问日志
--     INSERT INTO url_access_log_archive
--     SELECT * FROM url_access_log
--     WHERE access_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);
--
--     DELETE FROM url_access_log
--     WHERE access_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);
--
--     -- 归档操作日志
--     INSERT INTO user_operation_log_archive
--     SELECT * FROM user_operation_log
--     WHERE operation_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);
--
--     DELETE FROM user_operation_log
--     WHERE operation_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);
-- END;

-- =============================================
-- 7. 表结构说明
-- =============================================

-- short_url_mapping 表：
-- - short_key: 唯一索引，用于快速查找
-- - click_count: 统计访问次数
-- - status: 软删除设计
-- - expired_time: 支持短网址过期机制

-- url_access_log 表：
-- - 记录每次短网址访问的详细信息
-- - 支持按short_key、时间、IP等多维度查询
-- - 建议定期归档以保持表大小合理

-- user 表：
-- - password: BCrypt加密存储
-- - role: 支持USER/ADMIN角色
-- - status: 用户状态管理
-- - last_login_time: 记录最后登录时间

-- user_operation_log 表：
-- - 完整的操作审计日志
-- - 包含请求和响应信息
-- - 支持安全审计和故障排查
-- - 外键约束确保数据完整性