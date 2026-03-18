-- 短网址管理系统数据库初始化脚本
-- 适用数据库：MySQL 8.x

CREATE DATABASE IF NOT EXISTS short_url_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE short_url_db;

-- 1. 短链主表
CREATE TABLE IF NOT EXISTS `short_url_mapping` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `short_key` VARCHAR(20) NOT NULL COMMENT '短链 Key',
  `original_url` VARCHAR(2048) NOT NULL COMMENT '原始长链接',
  `title` VARCHAR(255) DEFAULT NULL COMMENT '标题',
  `click_count` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计点击数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=禁用',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `expired_time` DATETIME DEFAULT NULL COMMENT '过期时间，NULL 表示不过期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_key` (`short_key`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短链主表';

-- 2. 短链访问日志
CREATE TABLE IF NOT EXISTS `url_access_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `short_key` VARCHAR(20) NOT NULL COMMENT '短链 Key',
  `user_agent` TEXT DEFAULT NULL COMMENT '客户端 User-Agent',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '访问 IP',
  `access_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_short_key` (`short_key`),
  KEY `idx_access_time` (`access_time`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短链访问日志';

-- 3. 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER / ADMIN',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 4. 后台操作日志
CREATE TABLE IF NOT EXISTS `user_operation_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `operation_desc` VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
  `module` VARCHAR(50) NOT NULL COMMENT '所属模块',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '操作 IP',
  `user_agent` TEXT DEFAULT NULL COMMENT '客户端 User-Agent',
  `request_path` VARCHAR(500) DEFAULT NULL COMMENT '请求路径',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `response_result` TEXT DEFAULT NULL COMMENT '响应结果',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=成功，0=失败',
  `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_module` (`module`),
  CONSTRAINT `fk_user_operation_log_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台操作日志';

-- 5. 初始化管理员账号
-- 用户名：admin
-- 密码：admin123
INSERT INTO `user` (`username`, `password`, `email`, `role`, `status`)
VALUES ('admin', '$2a$12$VkcbkzVi1EepDMsQwOxBm.8dAn2/zRwJYDJY0LarGn.cbSPrXXbBK', 'admin@example.com', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE
  `email` = VALUES(`email`),
  `role` = VALUES(`role`),
  `status` = VALUES(`status`);
