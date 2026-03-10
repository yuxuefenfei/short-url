package com.example.shorturl.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短网址访问日志实体类
 * <p>
 * 模块职责：
 * - 对应数据库url_access_log表
 * - 记录每次短网址访问的详细信息
 * - 支持访问统计和分析
 * <p>
 * 表结构说明：
 * - short_key: 关联的短网址key
 * - user_agent: 用户浏览器信息
 * - ip_address: 访问者IP地址
 * - access_time: 访问时间
 * <p>
 * 依赖关系：
 * - 被StatsService使用进行统计分析
 * - 与ShortUrlMapping关联记录访问数据
 */
@Data
@Table("url_access_log")
public class UrlAccessLog {

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
     * 用户浏览器信息
     */
    @Column("user_agent")
    private String userAgent;

    /**
     * 访问者IP地址
     */
    @Column("ip_address")
    private String ipAddress;

    /**
     * 访问时间
     */
    @Column("access_time")
    private LocalDateTime accessTime;

    // ==================== 构造函数 ====================

    public UrlAccessLog() {
    }

    public UrlAccessLog(String shortKey, String userAgent, String ipAddress) {
        this.shortKey = shortKey;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.accessTime = LocalDateTime.now();
    }

    // ==================== 业务方法 ====================

    /**
     * 判断是否来自移动端
     */
    public boolean isMobile() {
        if (userAgent == null) {
            return false;
        }
        String lowerUserAgent = userAgent.toLowerCase();
        return lowerUserAgent.contains("mobile") ||
                lowerUserAgent.contains("android") ||
                lowerUserAgent.contains("iphone") ||
                lowerUserAgent.contains("ipad");
    }

    /**
     * 获取浏览器类型
     */
    public String getBrowserType() {
        if (userAgent == null) {
            return "Unknown";
        }

        String lowerUserAgent = userAgent.toLowerCase();
        if (lowerUserAgent.contains("chrome")) {
            return "Chrome";
        } else if (lowerUserAgent.contains("firefox")) {
            return "Firefox";
        } else if (lowerUserAgent.contains("safari")) {
            return "Safari";
        } else if (lowerUserAgent.contains("edge")) {
            return "Edge";
        } else if (lowerUserAgent.contains("opera")) {
            return "Opera";
        } else {
            return "Other";
        }
    }

    /**
     * 获取操作系统类型
     */
    public String getOperatingSystem() {
        if (userAgent == null) {
            return "Unknown";
        }

        String lowerUserAgent = userAgent.toLowerCase();
        if (lowerUserAgent.contains("windows")) {
            return "Windows";
        } else if (lowerUserAgent.contains("mac")) {
            return "macOS";
        } else if (lowerUserAgent.contains("linux")) {
            return "Linux";
        } else if (lowerUserAgent.contains("android")) {
            return "Android";
        } else if (lowerUserAgent.contains("iphone") || lowerUserAgent.contains("ipad")) {
            return "iOS";
        } else {
            return "Other";
        }
    }
}