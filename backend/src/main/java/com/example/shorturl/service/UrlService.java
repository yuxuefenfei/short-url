package com.example.shorturl.service;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.common.utils.ShortUrlGenerator;
import com.example.shorturl.dao.AccessLogDao;
import com.example.shorturl.dao.UrlMappingDao;
import com.example.shorturl.model.entity.ShortUrlMapping;
import com.example.shorturl.model.entity.table.ShortUrlMappingTableDef;
import com.example.shorturl.model.entity.table.UrlAccessLogTableDef;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 短网址服务类
 * <p>
 * 模块职责：
 * - 提供短网址生成和管理服务
 * - 处理短网址访问重定向
 * - 管理访问统计和缓存
 * <p>
 * 核心功能：
 * - 生成唯一的短网址key
 * - 存储短网址映射关系
 * - 处理访问重定向
 * - 记录访问日志
 * - Redis缓存管理
 * <p>
 * 性能优化：
 * - Redis缓存热点数据
 * - 异步日志记录
 * - 原子操作避免冲突
 * <p>
 * 依赖关系：
 * - 被UrlController调用
 * - 使用UrlMappingDao访问数据库
 * - 使用StringRedisTemplate管理缓存
 */
@Slf4j
@Service
public class UrlService {

    @Autowired
    private UrlMappingDao urlMappingDao;

    @Autowired
    private AccessLogDao accessLogDao;

    @Autowired
    private AsyncLogService asyncLogService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Value("${short-url.domain:https://short.ly}")
    private String shortUrlDomain;
    @Value("${short-url.cache-expire-days:7}")
    private int cacheExpireDays;

    /**
     * 获取URL映射DAO（仅供内部使用）
     */
    protected UrlMappingDao getUrlMappingDao() {
        return urlMappingDao;
    }

    /**
     * 清除URL缓存
     */
    public void clearUrlCache(String shortKey) {
        // 清除Redis缓存
        try {
            String cacheKey = "shortUrlMapping::" + shortKey;
            redisTemplate.delete(cacheKey);
            log.debug("缓存已清除: key={}", shortKey);
        } catch (Exception e) {
            log.warn("清除缓存失败: key={}, error={}", shortKey, e.getMessage());
        }
    }

    /**
     * 创建短网址
     *
     * @param originalUrl 原始URL
     * @param title       网址标题
     * @param expiredTime 过期时间
     * @return 短网址key
     */
    @Transactional
    public String createShortUrl(String originalUrl, String title, LocalDateTime expiredTime) {
        // 参数验证
        if (!StringUtils.hasText(originalUrl)) {
            throw new BusinessException(ResponseStatus.INVALID_URL_FORMAT);
        }

        if (originalUrl.length() > 2048) {
            throw new BusinessException(ResponseStatus.URL_LENGTH_EXCEEDED);
        }

        // 生成唯一的短网址key
        String shortKey = generateUniqueShortKey();

        // 创建短网址映射
        ShortUrlMapping mapping = new ShortUrlMapping();
        mapping.setShortKey(shortKey);
        mapping.setOriginalUrl(originalUrl);
        mapping.setTitle(title);
        mapping.setExpiredTime(expiredTime);
        mapping.setClickCount(0L);
        mapping.setStatus(1);

        // 保存到数据库
        urlMappingDao.insert(mapping);

        // 缓存会自动处理，通过@CachePut注解

        log.info("短网址创建成功: key={}, url={}", shortKey, originalUrl);

        return shortKey;
    }

    /**
     * 生成唯一的短网址key
     */
    private String generateUniqueShortKey() {
        String shortKey;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            shortKey = ShortUrlGenerator.generateShortKey();
            attempts++;

            // 使用Redis原子操作检查唯一性
            String key = "short_url_key:" + shortKey;
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", 24, TimeUnit.HOURS);

            if (Boolean.TRUE.equals(success)) {
                return shortKey;
            }

            // 如果Redis中已存在，检查数据库中是否真的存在
            ShortUrlMapping existing = urlMappingDao.selectOneByQuery(
                    QueryWrapper.create()
                            .where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.eq(shortKey))
            );

            if (existing == null) {
                // 数据库中不存在，可以安全使用
                return shortKey;
            }

        } while (attempts < maxAttempts);

        throw new BusinessException(ResponseStatus.SYSTEM_ERROR.getCode(),
                "无法生成唯一的短网址key，请稍后重试");
    }

    /**
     * 根据短网址key获取原始URL
     *
     * @param shortKey 短网址key
     * @return 原始URL，如果不存在或已过期返回null
     */
    @Cacheable(value = "shortUrlMapping", key = "#shortKey", unless = "#result == null")
    public String getOriginalUrl(String shortKey) {
        if (!ShortUrlGenerator.isValidShortKey(shortKey)) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }

        // 从数据库查询
        ShortUrlMapping mapping = urlMappingDao.selectOneByQuery(
                QueryWrapper.create()
                        .where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.eq(shortKey))
        );

        if (mapping == null) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }

        // 检查状态和过期时间
        if (!mapping.isAvailable()) {
            if (mapping.isExpired()) {
                throw new BusinessException(ResponseStatus.SHORT_URL_EXPIRED);
            } else {
                throw new BusinessException(ResponseStatus.SHORT_URL_DISABLED);
            }
        }

        // 异步更新访问统计
        asyncUpdateAccessStats(shortKey);

        return mapping.getOriginalUrl();
    }

    /**
     * 缓存短网址映射
     */
    private void cacheShortUrl(String shortKey, String originalUrl) {
        try {
            String cacheKey = "url:" + shortKey;
            redisTemplate.opsForValue().set(
                    cacheKey,
                    originalUrl,
                    cacheExpireDays,
                    TimeUnit.DAYS
            );
        } catch (Exception e) {
            log.warn("缓存短网址失败: key={}, error={}", shortKey, e.getMessage());
        }
    }

    /**
     * 异步更新访问统计
     */
    private void asyncUpdateAccessStats(String shortKey) {
        // 使用异步日志服务更新访问统计
        try {
            asyncLogService.updateClickCount(shortKey);
        } catch (Exception e) {
            log.error("异步更新访问统计失败: key={}, error={}", shortKey, e.getMessage());
        }
    }

    /**
     * 获取短网址统计信息
     *
     * @param shortKey 短网址key
     * @return 统计信息
     */
    public UrlStats getUrlStats(String shortKey) {
        ShortUrlMapping mapping = urlMappingDao.selectOneByQuery(
                QueryWrapper.create()
                        .where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.eq(shortKey))
        );

        if (mapping == null) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }

        UrlStats stats = new UrlStats();
        stats.setShortKey(shortKey);
        stats.setOriginalUrl(mapping.getOriginalUrl());
        stats.setTitle(mapping.getTitle());
        stats.setTotalClicks(mapping.getClickCount());
        stats.setCreatedTime(mapping.getCreatedTime());
        stats.setStatus(mapping.getStatus());

        // 查询今日访问次数
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long todayClicks = accessLogDao.selectCountByQuery(
                QueryWrapper.create()
                        .where(UrlAccessLogTableDef.URL_ACCESS_LOG.SHORT_KEY.eq(shortKey))
                        .and(UrlAccessLogTableDef.URL_ACCESS_LOG.ACCESS_TIME.ge(today))
        );
        stats.setTodayClicks(todayClicks);

        return stats;
    }

    /**
     * 获取短网址列表（分页）
     */
    public List<ShortUrlMapping> getUrlList(Integer page, Integer size, String keyword, Integer status) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.ORIGINAL_URL.like(keyword))
                    .or(ShortUrlMappingTableDef.SHORT_URL_MAPPING.TITLE.like(keyword))
                    .or(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.like(keyword));
        }

        if (status != null) {
            queryWrapper.where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.STATUS.eq(status));
        }

        queryWrapper.orderBy(ShortUrlMappingTableDef.SHORT_URL_MAPPING.CREATED_TIME, false);

        return urlMappingDao.selectListByQuery(queryWrapper);
    }

    /**
     * 获取短网址数量
     */
    public Long getUrlCount(String keyword, Integer status) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.ORIGINAL_URL.like(keyword))
                    .or(ShortUrlMappingTableDef.SHORT_URL_MAPPING.TITLE.like(keyword))
                    .or(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.like(keyword));
        }

        if (status != null) {
            queryWrapper.where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.STATUS.eq(status));
        }

        return urlMappingDao.selectCountByQuery(queryWrapper);
    }

    /**
     * 更新短网址状态
     */
    @Transactional
    public void updateUrlStatus(Long id, Integer status) {
        ShortUrlMapping mapping = urlMappingDao.selectOneById(id);
        if (mapping != null) {
            mapping.setStatus(status);
            mapping.setUpdatedTime(LocalDateTime.now());
            urlMappingDao.update(mapping);
            clearUrlCache(mapping.getShortKey());
        }
    }

    /**
     * 根据ID获取短网址
     */
    public ShortUrlMapping getUrlById(Long id) {
        if (id == null) {
            return null;
        }
        return urlMappingDao.selectOneById(id);
    }

    /**
     * 删除短网址
     */
    @Transactional
    public void deleteUrl(Long id) {
        ShortUrlMapping mapping = urlMappingDao.selectOneById(id);
        if (mapping != null) {
            urlMappingDao.deleteById(id);
            clearUrlCache(mapping.getShortKey());
        }
    }

    /**
     * 短网址统计信息类
     */
    @Setter
    @Getter
    public static class UrlStats {
        // getters and setters
        private String shortKey;
        private String originalUrl;
        private String title;
        private Long totalClicks;
        private Long todayClicks;
        private LocalDateTime createdTime;
        private Integer status;

    }
}