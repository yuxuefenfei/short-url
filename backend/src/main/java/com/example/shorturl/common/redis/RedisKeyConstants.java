package com.example.shorturl.common.redis;

/**
 * Redis Key 统一规范：
 * 1. 所有 Key 使用统一业务前缀 short_url
 * 2. 单词统一使用下划线风格
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String BUSINESS_PREFIX = "short_url";

    public static final String CACHE_PREFIX = BUSINESS_PREFIX + ":cache:";

    public static final String SHORT_URL_KEY_LOCK_PREFIX = BUSINESS_PREFIX + ":short_url_key_lock:";

    public static final String ONLINE_USERS_ZSET_KEY = BUSINESS_PREFIX + ":online_users";

    public static final String CSRF_TOKEN_KEY_PREFIX = BUSINESS_PREFIX + ":csrf_token:";
    public static final String CSRF_USER_TOKENS_KEY_PREFIX = BUSINESS_PREFIX + ":csrf_user_tokens:";

    public static final String RATE_LIMIT_KEY_PREFIX = BUSINESS_PREFIX + ":rate_limit:";

    public static final String IP_BLACKLIST_KEY_PREFIX = BUSINESS_PREFIX + ":ip_blacklist:";
    public static final String IP_BLACKLIST_SET_KEY = BUSINESS_PREFIX + ":ip_blacklist_set";
    public static final String IP_ATTEMPTS_KEY_PREFIX = BUSINESS_PREFIX + ":ip_attempts:";
    public static final String IP_BLOCK_TIME_KEY_PREFIX = BUSINESS_PREFIX + ":ip_block_time:";

    public static final String HEALTH_TEST_KEY_PREFIX = BUSINESS_PREFIX + ":health_test:";

    public static String buildRateLimitKey(String identifier, String apiPath) {
        String safeIdentifier = normalize(identifier);
        String safePath = normalize(apiPath);
        return RATE_LIMIT_KEY_PREFIX + safeIdentifier + ":" + safePath;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9]+", "_").toLowerCase();
    }
}
