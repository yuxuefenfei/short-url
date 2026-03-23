package com.example.shorturl.common.security;

import com.example.shorturl.common.redis.RedisKeyConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * IP黑名单过滤器
 * <p>
 * 模块职责：
 * - 基于IP地址的黑名单过滤
 * - 自动检测并封禁恶意IP
 * - 支持手动管理黑名单
 * - 防止恶意访问和攻击
 * <p>
 * 核心功能：
 * - IP黑名单检查
 * - 自动IP封禁
 * - 黑名单管理
 * - 封禁时间管理
 * <p>
 * 依赖关系：
 * - 在RateLimitFilter之前执行
 * - 依赖Redis存储黑名单
 */
@Slf4j
@Component
public class IpBlacklistFilter extends OncePerRequestFilter {

    // Redis键前缀
    private static final String BLACKLIST_KEY_PREFIX = RedisKeyConstants.IP_BLACKLIST_KEY_PREFIX;
    private static final String BLACKLIST_SET_KEY = RedisKeyConstants.IP_BLACKLIST_SET_KEY;
    private static final String IP_ATTEMPTS_KEY_PREFIX = RedisKeyConstants.IP_ATTEMPTS_KEY_PREFIX;
    private static final String IP_BLOCK_TIME_KEY_PREFIX = RedisKeyConstants.IP_BLOCK_TIME_KEY_PREFIX;
    // 封禁配置
    private static final int MAX_FAILED_ATTEMPTS = 10; // 最大失败尝试次数
    private static final int ATTEMPT_WINDOW_MINUTES = 10; // 尝试时间窗口(分钟)
    private static final int DEFAULT_BLOCK_DURATION_HOURS = 24; // 默认封禁时长(小时)
    private static final int PERMANENT_BLOCK_DURATION_DAYS = 365; // 永久封禁时长(天)
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String ipAddress = getClientIp(request);

            // 检查IP是否在黑名单中
            if (isIpBlocked(ipAddress)) {
                log.warn("黑名单IP访问被拒绝: ip={}, path={}", ipAddress, request.getRequestURI());
                sendBlockedResponse(response, ipAddress);
                return;
            }

            // 继续处理请求
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("IP黑名单过滤器执行失败: {}", e.getMessage(), e);
            // 过滤器失败时继续处理，避免影响正常业务
            filterChain.doFilter(request, response);
        }
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
     * 检查IP是否被封禁
     */
    private boolean isIpBlocked(String ipAddress) {
        try {
            // 检查永久黑名单
            Boolean isPermanentBlocked = redisTemplate.opsForSet().isMember(BLACKLIST_SET_KEY, ipAddress);
            if (Boolean.TRUE.equals(isPermanentBlocked)) {
                return true;
            }

            // 检查临时黑名单
            String blockKey = BLACKLIST_KEY_PREFIX + ipAddress;
            return redisTemplate.hasKey(blockKey);

        } catch (Exception e) {
            log.error("检查IP黑名单失败: ip={}, error={}", ipAddress, e.getMessage());
            return false; // Redis异常时允许通过
        }
    }

    /**
     * 发送封禁响应
     */
    private void sendBlockedResponse(HttpServletResponse response, String ipAddress) throws IOException {
        response.setStatus(403); // Forbidden
        response.setContentType("application/json;charset=UTF-8");

        Long blockExpireTime = getBlockExpireTime(ipAddress);
        String expireTimeStr = blockExpireTime != null ?
                LocalDateTime.now().plusSeconds(blockExpireTime).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) :
                "永久封禁";

        String jsonResponse = String.format(
                "{\"code\": 403, \"message\": \"您的IP已被封禁，解封时间: %s\", \"data\": null, \"timestamp\": \"%s\", \"requestId\": \"%s\"}",
                expireTimeStr,
                System.currentTimeMillis(),
                ""
        );

        response.getWriter().write(jsonResponse);
    }

    /**
     * 获取封禁剩余时间
     */
    private Long getBlockExpireTime(String ipAddress) {
        try {
            String blockKey = BLACKLIST_KEY_PREFIX + ipAddress;
            return redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取封禁时间失败: ip={}, error={}", ipAddress, e.getMessage());
            return null;
        }
    }

    /**
     * 封禁IP地址
     */
    public void blockIp(String ipAddress, int durationHours) {
        try {
            if (durationHours <= 0) {
                // 永久封禁
                redisTemplate.opsForSet().add(BLACKLIST_SET_KEY, ipAddress);
                log.info("IP永久封禁: ip={}", ipAddress);
            } else {
                // 临时封禁
                String blockKey = BLACKLIST_KEY_PREFIX + ipAddress;
                redisTemplate.opsForValue().set(blockKey, "blocked", durationHours, TimeUnit.HOURS);
                log.info("IP临时封禁: ip={}, duration={}小时", ipAddress, durationHours);
            }
        } catch (Exception e) {
            log.error("封禁IP失败: ip={}, error={}", ipAddress, e.getMessage());
        }
    }

    /**
     * 永久封禁IP
     */
    public void permanentBlockIp(String ipAddress) {
        blockIp(ipAddress, 0);
    }

    /**
     * 解封IP地址
     */
    public void unblockIp(String ipAddress) {
        try {
            // 移除永久黑名单
            redisTemplate.opsForSet().remove(BLACKLIST_SET_KEY, ipAddress);

            // 移除临时黑名单
            String blockKey = BLACKLIST_KEY_PREFIX + ipAddress;
            redisTemplate.delete(blockKey);

            // 清除尝试记录
            String attemptsKey = IP_ATTEMPTS_KEY_PREFIX + ipAddress;
            redisTemplate.delete(attemptsKey);

            // 清除封禁时间记录
            String blockTimeKey = IP_BLOCK_TIME_KEY_PREFIX + ipAddress;
            redisTemplate.delete(blockTimeKey);

            log.info("IP解封: ip={}", ipAddress);
        } catch (Exception e) {
            log.error("解封IP失败: ip={}, error={}", ipAddress, e.getMessage());
        }
    }

    /**
     * 记录失败尝试
     */
    public void recordFailedAttempt(String ipAddress) {
        try {
            String attemptsKey = IP_ATTEMPTS_KEY_PREFIX + ipAddress;
            long now = System.currentTimeMillis();

            // 添加失败尝试记录
            redisTemplate.opsForZSet().add(attemptsKey, String.valueOf(now), now);

            // 设置过期时间
            redisTemplate.expire(attemptsKey, ATTEMPT_WINDOW_MINUTES, TimeUnit.MINUTES);

            // 检查是否需要自动封禁
            checkAndAutoBlock(ipAddress);

        } catch (Exception e) {
            log.error("记录失败尝试: ip={}, error={}", ipAddress, e.getMessage());
        }
    }

    /**
     * 检查并自动封禁IP
     */
    private void checkAndAutoBlock(String ipAddress) {
        try {
            String attemptsKey = IP_ATTEMPTS_KEY_PREFIX + ipAddress;
            long now = System.currentTimeMillis();
            long windowStart = now - TimeUnit.MINUTES.toMillis(ATTEMPT_WINDOW_MINUTES);

            // 移除窗口外的记录
            redisTemplate.opsForZSet().removeRangeByScore(attemptsKey, 0, windowStart);

            // 获取窗口内的失败次数
            Long failedAttempts = redisTemplate.opsForZSet().size(attemptsKey);

            if (failedAttempts != null && failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // 自动封禁IP
                blockIp(ipAddress, DEFAULT_BLOCK_DURATION_HOURS);

                // 记录封禁时间
                String blockTimeKey = IP_BLOCK_TIME_KEY_PREFIX + ipAddress;
                redisTemplate.opsForValue().set(blockTimeKey,
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        DEFAULT_BLOCK_DURATION_HOURS, TimeUnit.HOURS);

                log.warn("IP自动封禁: ip={}, failed_attempts={}, duration={}小时",
                        ipAddress, failedAttempts, DEFAULT_BLOCK_DURATION_HOURS);
            }

        } catch (Exception e) {
            log.error("检查自动封禁失败: ip={}, error={}", ipAddress, e.getMessage());
        }
    }

    /**
     * 获取黑名单列表
     */
    public Set<String> getBlacklist() {
        try {
            return redisTemplate.opsForSet().members(BLACKLIST_SET_KEY);
        } catch (Exception e) {
            log.error("获取黑名单失败: error={}", e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    /**
     * 获取IP封禁信息
     */
    public IpBlockInfo getIpBlockInfo(String ipAddress) {
        try {
            boolean isPermanentBlocked = redisTemplate.opsForSet().isMember(BLACKLIST_SET_KEY, ipAddress);
            String blockKey = BLACKLIST_KEY_PREFIX + ipAddress;
            boolean isTemporaryBlocked = redisTemplate.hasKey(blockKey);

            Long remainingTime = null;
            if (isTemporaryBlocked) {
                remainingTime = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
            }

            String blockTimeKey = IP_BLOCK_TIME_KEY_PREFIX + ipAddress;
            String blockTime = redisTemplate.opsForValue().get(blockTimeKey);

            String attemptsKey = IP_ATTEMPTS_KEY_PREFIX + ipAddress;
            Long failedAttempts = redisTemplate.opsForZSet().size(attemptsKey);

            return new IpBlockInfo(ipAddress, isPermanentBlocked, isTemporaryBlocked,
                    remainingTime, blockTime, Objects.requireNonNullElse(failedAttempts, 0L));

        } catch (Exception e) {
            log.error("获取IP封禁信息失败: ip={}, error={}", ipAddress, e.getMessage());
            return null;
        }
    }

    /**
     * 清理过期封禁记录
     */
    public void cleanupExpiredBlocks() {
        try {
            // 获取所有临时封禁的IP
            Set<String> blockedIps = redisTemplate.keys(BLACKLIST_KEY_PREFIX + "*");

            if (blockedIps != null) {
                for (String key : blockedIps) {
                    if (!redisTemplate.hasKey(key)) {
                        // 已过期，清理相关记录
                        String ip = key.substring(BLACKLIST_KEY_PREFIX.length());
                        String attemptsKey = IP_ATTEMPTS_KEY_PREFIX + ip;
                        String blockTimeKey = IP_BLOCK_TIME_KEY_PREFIX + ip;

                        redisTemplate.delete(attemptsKey);
                        redisTemplate.delete(blockTimeKey);

                        log.debug("清理过期封禁记录: ip={}", ip);
                    }
                }
            }

            log.info("清理过期封禁记录完成");
        } catch (Exception e) {
            log.error("清理过期封禁记录失败: error={}", e.getMessage());
        }
    }

    /**
     * IP封禁信息类
     *
     * @param ipAddress Getters
     */
    public record IpBlockInfo(String ipAddress, boolean permanentBlocked, boolean temporaryBlocked,
                              Long remainingTimeSeconds, String blockTime, long failedAttempts) {

        public boolean isBlocked() {
            return permanentBlocked || temporaryBlocked;
        }

    }
}
