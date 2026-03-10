package com.example.shorturl.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * API访问限流过滤器
 * <p>
 * 模块职责：
 * - 基于IP地址的API访问频率限制
 * - 防止恶意请求和暴力破解
 * - 使用Redis实现分布式限流
 * - 支持不同API的不同限流策略
 * <p>
 * 核心功能：
 * - IP限流 (滑动窗口算法)
 * - API路径限流
 * - 用户限流 (基于Token)
 * - 灵活的限流配置
 * <p>
 * 依赖关系：
 * - 在Spring Security之前执行
 * - 依赖Redis进行计数
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // 默认限流配置
    private static final int DEFAULT_MAX_REQUESTS = 100; // 默认最大请求数
    private static final int DEFAULT_WINDOW_SECONDS = 60; // 默认时间窗口(秒)
    // 特殊API的限流配置
    private static final int LOGIN_MAX_REQUESTS = 10; // 登录接口限流
    private static final int REGISTER_MAX_REQUESTS = 5; // 注册接口限流
    private static final int SHORT_URL_MAX_REQUESTS = 50; // 短网址创建限流
    // Lua脚本：滑动窗口限流算法
    private static final String RATE_LIMIT_SCRIPT =
            """
                    local key = KEYS[1]
                    local now = tonumber(ARGV[1])
                    local window = tonumber(ARGV[2])
                    local limit = tonumber(ARGV[3])
                    
                    -- 移除窗口外的记录
                    redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
                    
                    -- 获取当前请求数
                    local current = redis.call('ZCARD', key)
                    
                    if current >= limit then
                        return 0
                    else
                        -- 添加当前请求
                        redis.call('ZADD', key, now, now .. ':' .. math.random())
                        -- 设置过期时间
                        redis.call('EXPIRE', key, window)
                        return 1
                    end""";
    private final RedisScript<Long> rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 获取客户端标识
            String identifier = getClientIdentifier(request);
            String apiPath = request.getRequestURI();

            // 检查是否需要限流
            if (shouldRateLimit(apiPath)) {
                // 获取限流配置
                RateLimitConfig config = getRateLimitConfig(apiPath);

                // 执行限流检查
                boolean allowed = checkRateLimit(identifier, apiPath, config);

                if (!allowed) {
                    log.warn("API访问频率超限: identifier={}, path={}, limit={}, window={}s",
                            identifier, apiPath, config.maxRequests, config.windowSeconds);

                    // 返回限流响应
                    sendRateLimitResponse(response, config);
                    return;
                }
            }

            // 继续处理请求
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("限流过滤器执行失败: {}", e.getMessage(), e);
            // 限流失败时继续处理，避免影响正常业务
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 获取客户端标识 (IP地址或用户ID)
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // 优先使用用户ID (已登录用户)
        String userId = (String) request.getAttribute("userId");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // 使用IP地址
        String ipAddress = getClientIp(request);
        return "ip:" + ipAddress;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 判断是否需要限流
     */
    private boolean shouldRateLimit(String apiPath) {
        // 排除静态资源和健康检查接口
        if (apiPath.startsWith("/static/") ||
                apiPath.startsWith("/assets/") ||
                apiPath.equals("/health") ||
                apiPath.equals("/favicon.ico")) {
            return false;
        }

        // 排除管理后台的部分只读接口
        if (apiPath.startsWith("/admin/logs") && "GET".equals(apiPath)) {
            return false;
        }

        return true;
    }

    /**
     * 获取限流配置
     */
    private RateLimitConfig getRateLimitConfig(String apiPath) {
        // 登录接口限流
        return switch (apiPath) {
            case "/api/auth/login" -> new RateLimitConfig(LOGIN_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);


            // 注册接口限流
            case "/api/auth/register" -> new RateLimitConfig(REGISTER_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);


            // 短网址创建限流
            case "/api/url/create" -> new RateLimitConfig(SHORT_URL_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);
            default ->

                // 默认限流配置
                    new RateLimitConfig(DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);
        };

    }

    /**
     * 检查限流
     */
    private boolean checkRateLimit(String identifier, String apiPath, RateLimitConfig config) {
        try {
            String key = "rate_limit:" + identifier + ":" + apiPath;
            long now = System.currentTimeMillis() / 1000;

            // 执行Lua脚本进行限流检查
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(config.windowSeconds),
                    String.valueOf(config.maxRequests)
            );

            return result != null && result == 1;

        } catch (Exception e) {
            log.error("限流检查失败: identifier={}, path={}, error={}",
                    identifier, apiPath, e.getMessage());
            // Redis异常时允许通过，避免影响正常业务
            return true;
        }
    }

    /**
     * 发送限流响应
     */
    private void sendRateLimitResponse(HttpServletResponse response, RateLimitConfig config) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json;charset=UTF-8");

        String jsonResponse = String.format(
                "{\"code\": 429, \"message\": \"请求过于频繁，请在%d秒后重试\", \"data\": null, \"timestamp\": \"%s\", \"requestId\": \"%s\"}",
                config.windowSeconds,
                System.currentTimeMillis(),
                ""
        );

        response.getWriter().write(jsonResponse);
    }

    /**
     * 清除指定标识的限流记录 (用于测试或特殊情况)
     */
    public void clearRateLimit(String identifier, String apiPath) {
        try {
            String key = "rate_limit:" + identifier + ":" + apiPath;
            redisTemplate.delete(key);
            log.info("清除限流记录: identifier={}, path={}", identifier, apiPath);
        } catch (Exception e) {
            log.error("清除限流记录失败: identifier={}, path={}, error={}",
                    identifier, apiPath, e.getMessage());
        }
    }

    /**
     * 获取限流统计信息
     */
    public RateLimitStats getRateLimitStats(String identifier, String apiPath) {
        try {
            String key = "rate_limit:" + identifier + ":" + apiPath;
            Long count = redisTemplate.opsForZSet().size(key);

            return new RateLimitStats(
                    identifier,
                    apiPath,
                    count != null ? count : 0,
                    getRateLimitConfig(apiPath)
            );
        } catch (Exception e) {
            log.error("获取限流统计失败: identifier={}, path={}, error={}",
                    identifier, apiPath, e.getMessage());
            return null;
        }
    }

    /**
     * 限流配置类
     */
    private record RateLimitConfig(int maxRequests, int windowSeconds) {
    }

    /**
     * 限流统计信息类
     */
    public record RateLimitStats(String identifier, String apiPath, long currentRequests, RateLimitConfig config) {

        public long getRemainingRequests() {
            return Math.max(0, config.maxRequests - currentRequests);
        }

        public double getUsagePercentage() {
            return config.maxRequests > 0 ? (double) currentRequests / config.maxRequests * 100 : 0;
        }
    }
}