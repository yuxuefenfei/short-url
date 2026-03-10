package com.example.shorturl.service;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.dao.AccessLogDao;
import com.example.shorturl.dao.UrlMappingDao;
import com.example.shorturl.model.entity.ShortUrlMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UrlService单元测试
 * <p>
 * 测试覆盖范围：
 * - 短网址创建功能
 * - URL重定向功能
 * - 访问统计功能
 * - 异常处理场景
 * - 缓存管理功能
 */
@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    private static final String TEST_DOMAIN = "https://short.ly";
    private static final int CACHE_EXPIRE_DAYS = 7;
    @Mock
    private UrlMappingDao urlMappingDao;
    @Mock
    private AccessLogDao accessLogDao;
    @Mock
    private AsyncLogService asyncLogService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        // 设置配置属性
        ReflectionTestUtils.setField(urlService, "shortUrlDomain", TEST_DOMAIN);
        ReflectionTestUtils.setField(urlService, "cacheExpireDays", CACHE_EXPIRE_DAYS);

        // 模拟Redis操作
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /**
     * 测试创建短网址 - 正常场景
     */
    @Test
    void testCreateShortUrl_Success() {
        // Given
        String originalUrl = "https://example.com/very/long/url";
        String title = "测试网址";
        LocalDateTime expiredTime = LocalDateTime.now().plusDays(30);
        String expectedShortKey = "abc123";

        // 模拟Redis setIfAbsent返回true（key不存在）
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // 模拟数据库插入
        doNothing().when(urlMappingDao).insert(any(ShortUrlMapping.class));

        // When
        String result = urlService.createShortUrl(originalUrl, title, expiredTime);

        // Then
        assertNotNull(result);
        verify(urlMappingDao).insert(any(ShortUrlMapping.class));
        verify(valueOperations).setIfAbsent(contains("short_url_key:"), anyString(), anyLong(), any(TimeUnit.class));
    }

    /**
     * 测试创建短网址 - URL为空
     */
    @Test
    void testCreateShortUrl_EmptyUrl_ThrowsException() {
        // Given
        String emptyUrl = "";

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> urlService.createShortUrl(emptyUrl, null, null));

        assertEquals(ResponseStatus.INVALID_URL_FORMAT.getCode(), exception.getCode());
        verify(urlMappingDao, never()).insert(any());
    }

    /**
     * 测试创建短网址 - URL过长
     */
    @Test
    void testCreateShortUrl_UrlTooLong_ThrowsException() {
        // Given
        String longUrl = "https://example.com/" + "a".repeat(2040); // 超过2048字符

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> urlService.createShortUrl(longUrl, null, null));

        assertEquals(ResponseStatus.URL_LENGTH_EXCEEDED.getCode(), exception.getCode());
        verify(urlMappingDao, never()).insert(any());
    }

    /**
     * 测试获取原始URL - 正常场景
     */
    @Test
    void testGetOriginalUrl_Success() {
        // Given
        String shortKey = "abc123";
        String originalUrl = "https://example.com/test";

        ShortUrlMapping mapping = new ShortUrlMapping();
        mapping.setShortKey(shortKey);
        mapping.setOriginalUrl(originalUrl);
        mapping.setStatus(1); // 正常状态
        mapping.setExpiredTime(LocalDateTime.now().plusDays(1)); // 未过期

        when(urlMappingDao.selectOneByQuery(any())).thenReturn(mapping);
        doNothing().when(asyncLogService).updateClickCount(anyString());

        // When
        String result = urlService.getOriginalUrl(shortKey);

        // Then
        assertEquals(originalUrl, result);
        verify(asyncLogService).updateClickCount(shortKey);
    }

    /**
     * 测试获取原始URL - 短网址不存在
     */
    @Test
    void testGetOriginalUrl_NotFound_ThrowsException() {
        // Given
        String shortKey = "nonexistent";

        when(urlMappingDao.selectOneByQuery(any())).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> urlService.getOriginalUrl(shortKey));

        assertEquals(ResponseStatus.SHORT_URL_NOT_EXIST.getCode(), exception.getCode());
    }

    /**
     * 测试获取原始URL - 短网址已过期
     */
    @Test
    void testGetOriginalUrl_Expired_ThrowsException() {
        // Given
        String shortKey = "expired123";

        ShortUrlMapping mapping = new ShortUrlMapping();
        mapping.setShortKey(shortKey);
        mapping.setOriginalUrl("https://example.com");
        mapping.setStatus(1);
        mapping.setExpiredTime(LocalDateTime.now().minusDays(1)); // 已过期

        when(urlMappingDao.selectOneByQuery(any())).thenReturn(mapping);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> urlService.getOriginalUrl(shortKey));

        assertEquals(ResponseStatus.SHORT_URL_EXPIRED.getCode(), exception.getCode());
    }

    /**
     * 测试获取原始URL - 短网址被禁用
     */
    @Test
    void testGetOriginalUrl_Disabled_ThrowsException() {
        // Given
        String shortKey = "disabled123";

        ShortUrlMapping mapping = new ShortUrlMapping();
        mapping.setShortKey(shortKey);
        mapping.setOriginalUrl("https://example.com");
        mapping.setStatus(0); // 禁用状态
        mapping.setExpiredTime(LocalDateTime.now().plusDays(1));

        when(urlMappingDao.selectOneByQuery(any())).thenReturn(mapping);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> urlService.getOriginalUrl(shortKey));

        assertEquals(ResponseStatus.SHORT_URL_DISABLED.getCode(), exception.getCode());
    }

    /**
     * 测试获取URL统计信息 - 正常场景
     */
    @Test
    void testGetUrlStats_Success() {
        // Given
        String shortKey = "stats123";
        String originalUrl = "https://example.com/stats";

        ShortUrlMapping mapping = new ShortUrlMapping();
        mapping.setShortKey(shortKey);
        mapping.setOriginalUrl(originalUrl);
        mapping.setTitle("统计测试");
        mapping.setClickCount(100L);
        mapping.setCreatedTime(LocalDateTime.now());
        mapping.setStatus(1);

        when(urlMappingDao.selectOneByQuery(any())).thenReturn(mapping);
        when(accessLogDao.selectCountByQuery(any())).thenReturn(25L); // 今日访问25次

        // When
        UrlService.UrlStats stats = urlService.getUrlStats(shortKey);

        // Then
        assertNotNull(stats);
        assertEquals(shortKey, stats.getShortKey());
        assertEquals(originalUrl, stats.getOriginalUrl());
        assertEquals("统计测试", stats.getTitle());
        assertEquals(100L, stats.getTotalClicks());
        assertEquals(25L, stats.getTodayClicks());
        assertEquals(1, stats.getStatus());
    }

    /**
     * 测试获取URL统计信息 - 短网址不存在
     */
    @Test
    void testGetUrlStats_NotFound_ThrowsException() {
        // Given
        String shortKey = "nonexistent";

        when(urlMappingDao.selectOneByQuery(any())).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> urlService.getUrlStats(shortKey));

        assertEquals(ResponseStatus.SHORT_URL_NOT_EXIST.getCode(), exception.getCode());
    }

    /**
     * 测试生成唯一短网址key的冲突处理
     */
    @Test
    void testCreateShortUrl_KeyConflict_RetrySuccess() {
        // Given
        String originalUrl = "https://example.com/conflict";
        String title = "冲突测试";

        // 模拟第一次Redis操作返回false（key已存在），第二次返回true
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false, true); // 第一次冲突，第二次成功

        // 模拟数据库查询返回（数据库中不存在）
        when(urlMappingDao.selectOneByQuery(any())).thenReturn(null);

        doNothing().when(urlMappingDao).insert(any(ShortUrlMapping.class));

        // When
        String result = urlService.createShortUrl(originalUrl, title, null);

        // Then
        assertNotNull(result);
        verify(urlMappingDao).insert(any(ShortUrlMapping.class));
        // 验证Redis操作被调用了两次
        verify(valueOperations, times(2)).setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    /**
     * 测试短网址key生成达到最大重试次数
     */
    @Test
    void testCreateShortUrl_MaxRetriesExceeded_ThrowsException() {
        // Given
        String originalUrl = "https://example.com/maxretry";

        // 模拟Redis操作始终返回false（总是冲突）
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        // 模拟数据库查询总是返回已存在的记录
        ShortUrlMapping existingMapping = new ShortUrlMapping();
        when(urlMappingDao.selectOneByQuery(any())).thenReturn(existingMapping);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> urlService.createShortUrl(originalUrl, null, null));

        assertTrue(exception.getMessage().contains("无法生成唯一的短网址key"));
        verify(urlMappingDao, never()).insert(any());
    }
}