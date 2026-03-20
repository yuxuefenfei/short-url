package com.example.shorturl.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Slf4j
@Component
public class LegacyRedisKeyMigrationRunner implements ApplicationRunner {

    private final StringRedisTemplate redisTemplate;

    @Value("${short-url.redis.migrate-legacy-keys-on-startup:true}")
    private boolean migrateOnStartup;

    public LegacyRedisKeyMigrationRunner(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!migrateOnStartup) {
            return;
        }

        Map<String, Function<String, String>> legacyPatternMap = new LinkedHashMap<>();
        legacyPatternMap.put("shortUrlMapping::*", this::mapLegacyCacheKey);
        legacyPatternMap.put("short_url_key:*", key -> mapByPrefix(key, "short_url_key:", RedisKeyConstants.SHORT_URL_KEY_LOCK_PREFIX));
        legacyPatternMap.put("online_users", key -> RedisKeyConstants.ONLINE_USERS_ZSET_KEY);
        legacyPatternMap.put("csrf_token:*", key -> mapByPrefix(key, "csrf_token:", RedisKeyConstants.CSRF_TOKEN_KEY_PREFIX));
        legacyPatternMap.put("csrf_user_tokens:*", key -> mapByPrefix(key, "csrf_user_tokens:", RedisKeyConstants.CSRF_USER_TOKENS_KEY_PREFIX));
        legacyPatternMap.put("rate_limit:*", this::mapLegacyRateLimitKey);
        legacyPatternMap.put("ip_blacklist:*", key -> mapByPrefix(key, "ip_blacklist:", RedisKeyConstants.IP_BLACKLIST_KEY_PREFIX));
        legacyPatternMap.put("ip_blacklist_set", key -> RedisKeyConstants.IP_BLACKLIST_SET_KEY);
        legacyPatternMap.put("ip_attempts:*", key -> mapByPrefix(key, "ip_attempts:", RedisKeyConstants.IP_ATTEMPTS_KEY_PREFIX));
        legacyPatternMap.put("ip_block_time:*", key -> mapByPrefix(key, "ip_block_time:", RedisKeyConstants.IP_BLOCK_TIME_KEY_PREFIX));
        legacyPatternMap.put("health:test:*", key -> mapByPrefix(key, "health:test:", RedisKeyConstants.HEALTH_TEST_KEY_PREFIX));

        int renamed = 0;
        int removed = 0;

        for (Map.Entry<String, Function<String, String>> entry : legacyPatternMap.entrySet()) {
            Set<String> keys = redisTemplate.keys(entry.getKey());
            if (keys == null || keys.isEmpty()) {
                continue;
            }
            for (String oldKey : keys) {
                String newKey = entry.getValue().apply(oldKey);
                if (newKey == null || newKey.equals(oldKey)) {
                    continue;
                }
                try {
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(newKey))) {
                        redisTemplate.delete(oldKey);
                        removed++;
                        continue;
                    }
                    Boolean success = redisTemplate.renameIfAbsent(oldKey, newKey);
                    if (Boolean.TRUE.equals(success)) {
                        renamed++;
                    } else {
                        redisTemplate.delete(oldKey);
                        removed++;
                    }
                } catch (Exception ex) {
                    log.warn("迁移 Redis Key 失败: oldKey={}, newKey={}, error={}", oldKey, newKey, ex.getMessage());
                }
            }
        }

        if (renamed > 0 || removed > 0) {
            log.info("Redis 历史 Key 迁移完成: renamed={}, removed={}", renamed, removed);
        }
    }

    private String mapLegacyCacheKey(String oldKey) {
        return mapByPrefix(oldKey, "shortUrlMapping::", RedisKeyConstants.CACHE_PREFIX + "short_url_mapping:");
    }

    private String mapLegacyRateLimitKey(String oldKey) {
        if (!oldKey.startsWith("rate_limit:")) {
            return oldKey;
        }
        String suffix = oldKey.substring("rate_limit:".length());
        int splitIndex = suffix.indexOf(':');
        if (splitIndex < 0) {
            return RedisKeyConstants.RATE_LIMIT_KEY_PREFIX + normalize(suffix) + ":unknown";
        }
        String identifier = suffix.substring(0, splitIndex);
        String apiPath = suffix.substring(splitIndex + 1);
        return RedisKeyConstants.buildRateLimitKey(identifier, apiPath);
    }

    private String mapByPrefix(String key, String oldPrefix, String newPrefix) {
        if (!key.startsWith(oldPrefix)) {
            return key;
        }
        return newPrefix + key.substring(oldPrefix.length());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9]+", "_").toLowerCase();
    }
}

