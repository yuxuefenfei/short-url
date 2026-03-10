package com.example.shorturl.controller;

import com.example.shorturl.common.response.ApiResponse;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 健康检查控制器
 * <p>
 * 模块职责：
 * - 提供应用健康状态检查接口
 * - 监控数据库连接状态
 * - 监控Redis连接状态
 * - 提供应用基本信息
 * <p>
 * 核心功能：
 * - 基础健康检查
 * - 数据库连接检查
 * - Redis连接检查
 * - 系统信息查询
 * <p>
 * 依赖关系：
 * - 被监控系统集成使用
 * - 依赖数据源和Redis连接
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${spring.application.name:short-url}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 基础健康检查接口
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();

        // 应用基本信息
        healthInfo.put("status", "UP");
        healthInfo.put("application", applicationName);
        healthInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // 检查数据库连接
        healthInfo.put("database", checkDatabaseHealth());

        // 检查Redis连接
        healthInfo.put("redis", checkRedisHealth());

        // 系统信息
        healthInfo.put("systemInfo", getSystemInfo());

        return ApiResponse.success(healthInfo);
    }

    /**
     * 详细健康检查接口
     */
    @GetMapping("/detailed")
    public ApiResponse<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> detailedInfo = new HashMap<>();

        detailedInfo.put("status", "UP");
        detailedInfo.put("application", applicationName);
        detailedInfo.put("serverPort", serverPort);
        detailedInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // 数据库详细检查
        detailedInfo.put("database", checkDatabaseDetailed());

        // Redis详细检查
        detailedInfo.put("redis", checkRedisDetailed());

        // JVM信息
        detailedInfo.put("jvm", getJvmInfo());

        // 内存信息
        detailedInfo.put("memory", getMemoryInfo());

        // 线程信息
        detailedInfo.put("threads", getThreadInfo());

        return ApiResponse.success(detailedInfo);
    }

    /**
     * 简单的存活检查
     */
    @GetMapping("/liveness")
    public ApiResponse<String> liveness() {
        return ApiResponse.success("Application is alive");
    }

    /**
     * 就绪检查
     */
    @GetMapping("/readiness")
    public ApiResponse<String> readiness() {
        // 检查关键依赖是否可用
        boolean dbHealthy = isDatabaseHealthy();
        boolean redisHealthy = isRedisHealthy();

        if (dbHealthy && redisHealthy) {
            return ApiResponse.success("Application is ready");
        } else {
            return ApiResponse.error(503, "Application is not ready");
        }
    }

    /**
     * 检查数据库健康状态
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbInfo = new HashMap<>();

        try {
            Connection connection = dataSource.getConnection();
            dbInfo.put("status", "UP");
            dbInfo.put("url", connection.getMetaData().getURL());
            dbInfo.put("username", connection.getMetaData().getUserName());
            connection.close();
        } catch (SQLException e) {
            dbInfo.put("status", "DOWN");
            dbInfo.put("error", e.getMessage());
            log.error("数据库连接检查失败", e);
        }

        return dbInfo;
    }

    /**
     * 检查Redis健康状态
     */
    private Map<String, Object> checkRedisHealth() {
        Map<String, Object> redisInfo = new HashMap<>();

        try {
            // 使用ping命令检查Redis连接
            String result = Objects.requireNonNull(Objects.requireNonNull(redisTemplate.getConnectionFactory())).getConnection().ping();
            redisInfo.put("status", "UP");
            redisInfo.put("response", result);
        } catch (Exception e) {
            redisInfo.put("status", "DOWN");
            redisInfo.put("error", e.getMessage());
            log.error("Redis连接检查失败", e);
        }

        return redisInfo;
    }

    /**
     * 详细数据库检查
     */
    private Map<String, Object> checkDatabaseDetailed() {
        Map<String, Object> dbInfo = new HashMap<>();

        try {
            Connection connection = dataSource.getConnection();
            dbInfo.put("status", "UP");
            dbInfo.put("url", connection.getMetaData().getURL());
            dbInfo.put("username", connection.getMetaData().getUserName());
            dbInfo.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
            dbInfo.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
            dbInfo.put("driverName", connection.getMetaData().getDriverName());
            dbInfo.put("driverVersion", connection.getMetaData().getDriverVersion());

            // 检查连接池状态
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
                if (pool != null) {
                    dbInfo.put("activeConnections", pool.getActiveConnections());
                    dbInfo.put("idleConnections", pool.getIdleConnections());
                    dbInfo.put("totalConnections", pool.getTotalConnections());
                    dbInfo.put("threadsAwaitingConnection", pool.getThreadsAwaitingConnection());
                }
            }

            connection.close();
        } catch (SQLException e) {
            dbInfo.put("status", "DOWN");
            dbInfo.put("error", e.getMessage());
            log.error("数据库详细检查失败", e);
        }

        return dbInfo;
    }

    /**
     * 详细Redis检查
     */
    private Map<String, Object> checkRedisDetailed() {
        Map<String, Object> redisInfo = new HashMap<>();

        try {
            // 设置测试key
            String testKey = "health:test:" + System.currentTimeMillis();
            String testValue = "test_value";

            // 测试set操作
            redisTemplate.opsForValue().set(testKey, testValue, 10, TimeUnit.SECONDS);

            // 测试get操作
            String retrievedValue = redisTemplate.opsForValue().get(testKey);

            // 测试delete操作
            redisTemplate.delete(testKey);

            redisInfo.put("status", "UP");
            redisInfo.put("setOperation", "SUCCESS");
            redisInfo.put("getOperation", retrievedValue != null ? "SUCCESS" : "FAILED");
            redisInfo.put("deleteOperation", "SUCCESS");
            redisInfo.put("testValue", testValue);
            redisInfo.put("retrievedValue", retrievedValue);

        } catch (Exception e) {
            redisInfo.put("status", "DOWN");
            redisInfo.put("error", e.getMessage());
            log.error("Redis详细检查失败", e);
        }

        return redisInfo;
    }

    /**
     * 获取系统信息
     */
    private Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();

        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("osArch", System.getProperty("os.arch"));
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("javaVendor", System.getProperty("java.vendor"));
        systemInfo.put("userName", System.getProperty("user.name"));
        systemInfo.put("userHome", System.getProperty("user.home"));
        systemInfo.put("userDir", System.getProperty("user.dir"));
        systemInfo.put("fileEncoding", Charset.defaultCharset().displayName());

        return systemInfo;
    }

    /**
     * 获取JVM信息
     */
    private Map<String, Object> getJvmInfo() {
        Map<String, Object> jvmInfo = new HashMap<>();

        Runtime runtime = Runtime.getRuntime();

        jvmInfo.put("version", System.getProperty("java.version"));
        jvmInfo.put("vendor", System.getProperty("java.vendor"));
        jvmInfo.put("vmName", System.getProperty("java.vm.name"));
        jvmInfo.put("vmVersion", System.getProperty("java.vm.version"));
        jvmInfo.put("availableProcessors", runtime.availableProcessors());
        jvmInfo.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());

        return jvmInfo;
    }

    /**
     * 获取内存信息
     */
    private Map<String, Object> getMemoryInfo() {
        Map<String, Object> memoryInfo = new HashMap<>();

        Runtime runtime = Runtime.getRuntime();

        memoryInfo.put("totalMemory", runtime.totalMemory());
        memoryInfo.put("freeMemory", runtime.freeMemory());
        memoryInfo.put("maxMemory", runtime.maxMemory());
        memoryInfo.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());

        // 转换为MB显示
        memoryInfo.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
        memoryInfo.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
        memoryInfo.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
        memoryInfo.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);

        return memoryInfo;
    }

    /**
     * 获取线程信息
     */
    private Map<String, Object> getThreadInfo() {
        Map<String, Object> threadInfo = new HashMap<>();

        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }

        Thread[] threads = new Thread[rootGroup.activeCount()];
        rootGroup.enumerate(threads);

        threadInfo.put("activeThreads", rootGroup.activeCount());
        threadInfo.put("peakThreadCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
        threadInfo.put("totalStartedThreadCount", ManagementFactory.getThreadMXBean().getTotalStartedThreadCount());

        return threadInfo;
    }

    /**
     * 检查数据库是否健康
     */
    private boolean isDatabaseHealthy() {
        try {
            Connection connection = dataSource.getConnection();
            connection.close();
            return true;
        } catch (SQLException e) {
            log.error("数据库健康检查失败", e);
            return false;
        }
    }

    /**
     * 检查Redis是否健康
     */
    private boolean isRedisHealthy() {
        try {
            Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().ping();
            return true;
        } catch (Exception e) {
            log.error("Redis健康检查失败", e);
            return false;
        }
    }
}
