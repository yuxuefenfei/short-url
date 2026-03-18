package com.example.shorturl.service;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.dao.AccessLogDao;
import com.example.shorturl.dao.UrlMappingDao;
import com.example.shorturl.model.entity.ShortUrlMapping;
import com.example.shorturl.model.entity.UrlAccessLog;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

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
        ReflectionTestUtils.setField(urlService, "shortUrlDomain", "https://short.ly");
        ReflectionTestUtils.setField(urlService, "cacheExpireDays", 7);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testCreateShortUrlSuccess() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(urlMappingDao.insert(any(ShortUrlMapping.class))).thenReturn(1);

        String shortKey = urlService.createShortUrl("https://example.com", "test", LocalDateTime.now().plusDays(1));

        assertNotNull(shortKey);
        verify(urlMappingDao).insert(any(ShortUrlMapping.class));
    }

    @Test
    void testCreateShortUrlEmptyUrl() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> urlService.createShortUrl("", null, null)
        );

        assertEquals(ResponseStatus.INVALID_URL_FORMAT.getCode(), exception.getCode());
        verify(urlMappingDao, never()).insert(any(ShortUrlMapping.class));
    }

    @Test
    void testGetOriginalUrlSuccess() {
        ShortUrlMapping mapping = new ShortUrlMapping();
        mapping.setShortKey("abc123");
        mapping.setOriginalUrl("https://example.com/test");
        mapping.setStatus(1);
        mapping.setExpiredTime(LocalDateTime.now().plusDays(1));

        when(urlMappingDao.selectOneByQuery(any())).thenReturn(mapping);

        String result = urlService.getOriginalUrl("abc123");

        assertEquals("https://example.com/test", result);
        verify(asyncLogService).updateClickCount("abc123");
    }

    @Test
    void testGetOriginalUrlNotFound() {
        when(urlMappingDao.selectOneByQuery(any())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> urlService.getOriginalUrl("abc123")
        );

        assertEquals(ResponseStatus.SHORT_URL_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    void testGetUrlStatsSuccess() {
        ShortUrlMapping mapping = new ShortUrlMapping();
        mapping.setShortKey("abc123");
        mapping.setOriginalUrl("https://example.com/test");
        mapping.setTitle("stats");
        mapping.setClickCount(100L);
        mapping.setCreatedTime(LocalDateTime.now());
        mapping.setStatus(1);

        UrlAccessLog lastLog = new UrlAccessLog();
        lastLog.setAccessTime(LocalDateTime.now());

        when(urlMappingDao.selectOneByQuery(any())).thenReturn(mapping);
        when(accessLogDao.selectCountByQuery(any())).thenReturn(25L);
        when(accessLogDao.selectListByQuery(any())).thenReturn(List.of(lastLog));

        UrlService.UrlStats stats = urlService.getUrlStats("abc123");

        assertEquals("abc123", stats.getShortKey());
        assertEquals(100L, stats.getTotalClicks());
        assertEquals(25L, stats.getTodayClicks());
        assertNotNull(stats.getLastAccessTime());
    }

    @Test
    void testCreateShortUrlKeyConflictThenSuccess() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false, true);
        when(urlMappingDao.selectOneByQuery(any())).thenReturn(new ShortUrlMapping(), null);
        when(urlMappingDao.insert(any(ShortUrlMapping.class))).thenReturn(1);

        String shortKey = urlService.createShortUrl("https://example.com", "test", null);

        assertNotNull(shortKey);
        verify(valueOperations, times(2)).setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }
}
