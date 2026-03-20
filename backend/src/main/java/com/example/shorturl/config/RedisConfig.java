package com.example.shorturl.config;

import com.example.shorturl.common.redis.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置
 * <p>
 * 模块职责：
 * - 配置Redis缓存策略
 * - 优化缓存序列化
 * - 设置缓存过期时间
 * - 提高缓存性能
 * <p>
 * 核心功能：
 * - 多级缓存配置
 * - JSON序列化优化
 * - 缓存过期策略
 * - RedisTemplate配置
 * <p>
 * 依赖关系：
 * - 被Spring Cache抽象层使用
 * - 支持服务层的缓存注解
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 配置RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 设置key的序列化器为String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 设置value的序列化器为JSON
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        // 基础缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 默认1小时过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .computePrefixWith(cacheName -> RedisKeyConstants.CACHE_PREFIX + cacheName + ":")
                .disableCachingNullValues();

        // 自定义缓存配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 短网址映射缓存 - 24小时
        cacheConfigurations.put("short_url_mapping", defaultConfig.entryTtl(Duration.ofHours(24)));

        // 用户信息缓存 - 12小时
        cacheConfigurations.put("user_info", defaultConfig.entryTtl(Duration.ofHours(12)));

        // 访问统计缓存 - 5分钟
        cacheConfigurations.put("access_stats", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 操作日志缓存 - 30分钟
        cacheConfigurations.put("operation_logs", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 黑名单缓存 - 永久
        cacheConfigurations.put("blacklist", defaultConfig.entryTtl(Duration.ofDays(365)));

        // 限流缓存 - 1小时
        cacheConfigurations.put("rate_limit", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
