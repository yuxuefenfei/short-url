package com.example.shorturl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 在线用户统计服务
 *
 * 使用 Redis ZSet 记录用户最近活跃时间，按 TTL 过期窗口计算在线人数。
 */
@Slf4j
@Service
public class OnlineUserService {

    private static final String ONLINE_USER_ZSET_KEY = "online_users";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${admin.online-user.ttl-seconds:1800}")
    private long onlineUserTtlSeconds;

    public void markUserOnline(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            long now = System.currentTimeMillis();
            cleanupExpired(now);
            redisTemplate.opsForZSet().add(ONLINE_USER_ZSET_KEY, String.valueOf(userId), now);
        } catch (Exception e) {
            log.warn("更新在线用户心跳失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    public long countOnlineUsers() {
        try {
            cleanupExpired(System.currentTimeMillis());
            Long count = redisTemplate.opsForZSet().zCard(ONLINE_USER_ZSET_KEY);
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("统计在线用户失败: {}", e.getMessage());
            return 0L;
        }
    }

    public void markUserOffline(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            redisTemplate.opsForZSet().remove(ONLINE_USER_ZSET_KEY, String.valueOf(userId));
        } catch (Exception e) {
            log.warn("移除在线用户失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    private void cleanupExpired(long nowMillis) {
        long expireBefore = nowMillis - (onlineUserTtlSeconds * 1000L);
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_USER_ZSET_KEY, 0, expireBefore);
    }
}
