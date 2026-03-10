package com.example.shorturl.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短网址映射实体类
 * <p>
 * 模块职责：
 * - 对应数据库short_url_mapping表
 * - 存储短网址与原始URL的映射关系
 * - 记录访问统计和状态信息
 * <p>
 * 表结构说明：
 * - short_key: 短网址key，唯一索引
 * - original_url: 原始长网址
 * - click_count: 访问次数统计
 * - status: 状态管理（1正常/0禁用）
 * - expired_time: 支持过期机制
 * <p>
 * 依赖关系：
 * - 被UrlService和UrlController使用
 * - 与UrlAccessLog关联记录访问日志
 */
@Data
@Table("short_url_mapping")
public class ShortUrlMapping {

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 短网址key
     */
    @Column("short_key")
    private String shortKey;

    /**
     * 原始长网址
     */
    @Column("original_url")
    private String originalUrl;

    /**
     * 网址标题
     */
    private String title;

    /**
     * 点击次数
     */
    @Column("click_count")
    private Long clickCount;

    /**
     * 状态：1正常，0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @Column("created_time")
    private LocalDateTime createdTime;

    /**
     * 过期时间，NULL表示永不过期
     */
    @Column("expired_time")
    private LocalDateTime expiredTime;

    /**
     * 更新时间
     */
    @Column("updated_time")
    private LocalDateTime updatedTime;

    // ==================== 构造函数 ====================

    public ShortUrlMapping() {
        this.clickCount = 0L;
        this.status = 1;
    }

    // ==================== 业务方法 ====================

    /**
     * 判断短网址是否过期
     */
    public boolean isExpired() {
        if (expiredTime == null) {
            return false; // 永不过期
        }
        return LocalDateTime.now().isAfter(expiredTime);
    }

    /**
     * 判断短网址是否可用（正常状态且未过期）
     */
    public boolean isAvailable() {
        return status != null && status == 1 && !isExpired();
    }

    /**
     * 增加点击计数
     */
    public void incrementClickCount() {
        if (this.clickCount == null) {
            this.clickCount = 0L;
        }
        this.clickCount++;
    }

    /**
     * 禁用短网址
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 启用短网址
     */
    public void enable() {
        this.status = 1;
    }
}