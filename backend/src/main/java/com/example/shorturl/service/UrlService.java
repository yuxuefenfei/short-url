package com.example.shorturl.service;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.redis.RedisKeyConstants;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.common.utils.ShortUrlGenerator;
import com.example.shorturl.common.utils.PageUtils;
import com.example.shorturl.config.AppConfig;
import com.example.shorturl.dao.AccessLogDao;
import com.example.shorturl.dao.UrlMappingDao;
import com.example.shorturl.model.entity.ShortUrlMapping;
import com.example.shorturl.model.entity.table.ShortUrlMappingTableDef;
import com.example.shorturl.model.entity.table.UrlAccessLogTableDef;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.mybatisflex.core.query.QueryMethods.sum;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {
    private final UrlMappingDao urlMappingDao;

    private final AccessLogDao accessLogDao;

    private final StringRedisTemplate redisTemplate;

    private final AppConfig appConfig;

    @Transactional
    public String createShortUrl(String originalUrl, String title, LocalDateTime expiredTime) {
        if (!StringUtils.hasText(originalUrl)) {
            throw new BusinessException(ResponseStatus.INVALID_URL_FORMAT);
        }
        if (originalUrl.length() > 2048) {
            throw new BusinessException(ResponseStatus.URL_LENGTH_EXCEEDED);
        }

        String shortKey = generateUniqueShortKey();
        ShortUrlMapping mapping = new ShortUrlMapping();
        mapping.setShortKey(shortKey);
        mapping.setOriginalUrl(originalUrl);
        mapping.setTitle(title);
        mapping.setExpiredTime(expiredTime);
        mapping.setClickCount(0L);
        mapping.setStatus(1);
        mapping.setCreatedTime(LocalDateTime.now());
        mapping.setUpdatedTime(LocalDateTime.now());

        urlMappingDao.insert(mapping);
        return shortKey;
    }

    @Cacheable(value = "short_url_mapping", key = "#shortKey", unless = "#result == null")
    public String getOriginalUrl(String shortKey) {
        if (!ShortUrlGenerator.isValidShortKey(shortKey)) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }

        ShortUrlMapping mapping = urlMappingDao.selectOneByQuery(
                QueryWrapper.create().where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.eq(shortKey))
        );
        if (mapping == null) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }
        if (!mapping.isAvailable()) {
            if (mapping.isExpired()) {
                throw new BusinessException(ResponseStatus.SHORT_URL_EXPIRED);
            }
            throw new BusinessException(ResponseStatus.SHORT_URL_DISABLED);
        }

        return mapping.getOriginalUrl();
    }

    public UrlStats getUrlStats(String shortKey) {
        ShortUrlMapping mapping = urlMappingDao.selectOneByQuery(
                QueryWrapper.create().where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.eq(shortKey))
        );
        if (mapping == null) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }

        UrlStats stats = new UrlStats();
        stats.setShortKey(shortKey);
        stats.setShortUrl(appConfig.getShortUrl().getDomain() + "/" + shortKey);
        stats.setOriginalUrl(mapping.getOriginalUrl());
        stats.setTitle(mapping.getTitle());
        stats.setTotalClicks(mapping.getClickCount());
        stats.setCreatedTime(mapping.getCreatedTime());
        stats.setLastAccessTime(getLastAccessTime(shortKey));
        stats.setStatus(mapping.getStatus());
        stats.setTodayClicks(getTodayClicks(shortKey));
        stats.setTrend(getTrend(shortKey, 7));
        stats.setAccessSources(getAccessSources(shortKey));
        return stats;
    }

    public List<ShortUrlMapping> getUrlList(Integer page, Integer size, String keyword, Integer status) {
        QueryWrapper queryWrapper = buildUrlQuery(keyword, status);
        queryWrapper.orderBy(ShortUrlMappingTableDef.SHORT_URL_MAPPING.CREATED_TIME, false);
        Page<ShortUrlMapping> result = urlMappingDao.paginate(
                PageUtils.safePage(page, appConfig.getPagination()),
                PageUtils.safeSize(size, appConfig.getPagination()),
                queryWrapper
        );
        return PageUtils.records(result);
    }

    public Long getUrlCount(String keyword, Integer status) {
        return urlMappingDao.selectCountByQuery(buildUrlQuery(keyword, status));
    }

    @Transactional
    public void updateUrlStatus(Long id, Integer status) {
        validateStatus(status);
        ShortUrlMapping mapping = urlMappingDao.selectOneById(id);
        if (mapping == null) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }
        mapping.setStatus(status);
        mapping.setUpdatedTime(LocalDateTime.now());
        urlMappingDao.update(mapping);
        clearUrlCache(mapping.getShortKey());
    }

    public ShortUrlMapping getUrlById(Long id) {
        ShortUrlMapping mapping = Optional.ofNullable(id)
                .map(urlMappingDao::selectOneById)
                .orElse(null);
        if (mapping == null) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }
        return mapping;
    }

    @Transactional
    public ShortUrlMapping updateUrl(Long id, String title, LocalDateTime expiredTime) {
        ShortUrlMapping mapping = urlMappingDao.selectOneById(id);
        if (mapping == null) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }

        if (title != null) {
            mapping.setTitle(title);
        }
        mapping.setExpiredTime(expiredTime);
        mapping.setUpdatedTime(LocalDateTime.now());
        urlMappingDao.update(mapping);
        clearUrlCache(mapping.getShortKey());
        return mapping;
    }

    @Transactional
    public void deleteUrl(Long id) {
        ShortUrlMapping mapping = urlMappingDao.selectOneById(id);
        if (mapping == null) {
            throw new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST);
        }
        urlMappingDao.deleteById(id);
        clearUrlCache(mapping.getShortKey());
    }

    @Transactional(readOnly = true)
    public long getTotalClicks() {
        Long totalClicks = urlMappingDao.selectObjectByQueryAs(
                QueryWrapper.create().select(sum(ShortUrlMappingTableDef.SHORT_URL_MAPPING.CLICK_COUNT)),
                Long.class
        );
        return Objects.requireNonNullElse(totalClicks, 0L);
    }

    @Transactional(readOnly = true)
    public long getTodayClicks() {
        return accessLogDao.selectCountByQuery(
                QueryWrapper.create().where(UrlAccessLogTableDef.URL_ACCESS_LOG.ACCESS_TIME.ge(LocalDate.now().atStartOfDay()))
        );
    }

    @Transactional(readOnly = true)
    public long getTodayNewUrls() {
        return urlMappingDao.selectCountByQuery(
                QueryWrapper.create().where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.CREATED_TIME.ge(LocalDate.now().atStartOfDay()))
        );
    }

    public void clearUrlCache(String shortKey) {
        try {
            redisTemplate.delete(RedisKeyConstants.BUSINESS_PREFIX + "short_url_mapping:" + shortKey);
        } catch (Exception e) {
            log.warn("清理缓存失败: key={}, error={}", shortKey, e.getMessage());
        }
    }

    private QueryWrapper buildUrlQuery(String keyword, Integer status) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.ORIGINAL_URL.like(keyword)
                    .or(ShortUrlMappingTableDef.SHORT_URL_MAPPING.TITLE.like(keyword))
                    .or(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.like(keyword)));
        }
        if (status != null) {
            queryWrapper.and(ShortUrlMappingTableDef.SHORT_URL_MAPPING.STATUS.eq(status));
        }
        return queryWrapper;
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "短链状态只能为 0 或 1");
        }
    }

    private Long getTodayClicks(String shortKey) {
        return accessLogDao.selectCountByQuery(
                QueryWrapper.create()
                        .where(UrlAccessLogTableDef.URL_ACCESS_LOG.SHORT_KEY.eq(shortKey))
                        .and(UrlAccessLogTableDef.URL_ACCESS_LOG.ACCESS_TIME.ge(LocalDate.now().atStartOfDay()))
        );
    }

    private LocalDateTime getLastAccessTime(String shortKey) {
        List<com.example.shorturl.model.entity.UrlAccessLog> logs = accessLogDao.selectListByQuery(
                QueryWrapper.create()
                        .where(UrlAccessLogTableDef.URL_ACCESS_LOG.SHORT_KEY.eq(shortKey))
                        .orderBy(UrlAccessLogTableDef.URL_ACCESS_LOG.ACCESS_TIME, false)
        );
        return logs.stream()
                .findFirst()
                .map(com.example.shorturl.model.entity.UrlAccessLog::getAccessTime)
                .orElse(null);
    }

    private List<DailyClickPoint> getTrend(String shortKey, int days) {
        int safeDays = Math.max(days, 1);
        LocalDate start = LocalDate.now().minusDays(safeDays - 1L);
        Map<LocalDate, Long> trendMap = new LinkedHashMap<>();
        for (int i = 0; i < safeDays; i++) {
            trendMap.put(start.plusDays(i), 0L);
        }

        List<com.example.shorturl.model.entity.UrlAccessLog> logs = accessLogDao.selectListByQuery(
                QueryWrapper.create()
                        .where(UrlAccessLogTableDef.URL_ACCESS_LOG.SHORT_KEY.eq(shortKey))
                        .and(UrlAccessLogTableDef.URL_ACCESS_LOG.ACCESS_TIME.ge(start.atStartOfDay()))
        );
        for (com.example.shorturl.model.entity.UrlAccessLog log : logs) {
            LocalDate accessDate = log.getAccessTime().toLocalDate();
            if (trendMap.containsKey(accessDate)) {
                trendMap.put(accessDate, trendMap.get(accessDate) + 1);
            }
        }

        List<DailyClickPoint> trend = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : trendMap.entrySet()) {
            DailyClickPoint point = new DailyClickPoint();
            point.setDate(entry.getKey().format(DateTimeFormatter.ISO_DATE));
            point.setClicks(entry.getValue());
            trend.add(point);
        }
        return trend;
    }

    private List<AccessSourceItem> getAccessSources(String shortKey) {
        List<com.example.shorturl.model.entity.UrlAccessLog> logs = accessLogDao.selectListByQuery(
                QueryWrapper.create().where(UrlAccessLogTableDef.URL_ACCESS_LOG.SHORT_KEY.eq(shortKey))
        );
        if (logs.isEmpty()) {
            return List.of();
        }

        Map<String, Long> sourceMap = new LinkedHashMap<>();
        for (com.example.shorturl.model.entity.UrlAccessLog log : logs) {
            String source = log.getBrowserType();
            sourceMap.put(source, sourceMap.getOrDefault(source, 0L) + 1);
        }

        return sourceMap.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .map(entry -> {
                    AccessSourceItem item = new AccessSourceItem();
                    item.setSource(entry.getKey());
                    item.setCount(entry.getValue());
                    return item;
                })
                .toList();
    }

    private String generateUniqueShortKey() {
        for (int attempts = 0; attempts < 10; attempts++) {
            String shortKey = ShortUrlGenerator.generateShortKey();
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    RedisKeyConstants.SHORT_URL_KEY_LOCK_PREFIX + shortKey, "1", 24, TimeUnit.HOURS
            );
            ShortUrlMapping existing = urlMappingDao.selectOneByQuery(
                    QueryWrapper.create().where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.eq(shortKey))
            );
            if (Boolean.TRUE.equals(success) && existing == null) {
                return shortKey;
            }
        }

        throw new BusinessException(ResponseStatus.SYSTEM_ERROR.getCode(), "无法生成唯一的短链接，请稍后重试");
    }

    @Setter
    @Getter
    public static class UrlStats {
        private String shortKey;
        private String shortUrl;
        private String originalUrl;
        private String title;
        private Long totalClicks;
        private Long todayClicks;
        private LocalDateTime createdTime;
        private LocalDateTime lastAccessTime;
        private Integer status;
        private List<DailyClickPoint> trend;
        private List<AccessSourceItem> accessSources;
    }

    @Setter
    @Getter
    public static class DailyClickPoint {
        private String date;
        private Long clicks;
    }

    @Setter
    @Getter
    public static class AccessSourceItem {
        private String source;
        private Long count;
    }
}
