package com.example.shorturl.controller;

import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.service.UrlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * URL控制器集成测试
 * <p>
 * 测试覆盖范围：
 * - 短网址创建API
 * - 短网址访问API
 * - 短网址统计API
 * - 错误处理场景
 */
@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlService urlService;

    private Map<String, Object> createShortUrlRequest;

    @BeforeEach
    void setUp() {
        createShortUrlRequest = new HashMap<>();
        createShortUrlRequest.put("originalUrl", "https://example.com/very/long/url");
        createShortUrlRequest.put("title", "测试网址");
        createShortUrlRequest.put("expiredAt", LocalDateTime.now().plusDays(30));
    }

    /**
     * 测试创建短网址API - 成功场景
     */
    @Test
    void testCreateShortUrl_Success() throws Exception {
        // Given
        String expectedShortKey = "abc123";
        when(urlService.createShortUrl(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(expectedShortKey);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/url/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createShortUrlRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ResponseStatus.SUCCESS.getCode()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.shortKey").value(expectedShortKey))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.shortUrl").exists());
    }

    /**
     * 测试创建短网址API - URL为空
     */
    @Test
    void testCreateShortUrl_EmptyUrl() throws Exception {
        // Given
        createShortUrlRequest.put("originalUrl", "");

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/url/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createShortUrlRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ResponseStatus.INVALID_URL_FORMAT.getCode()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists());
    }

    /**
     * 测试短网址访问API - 成功重定向
     */
    @Test
    void testRedirectUrl_Success() throws Exception {
        // Given
        String shortKey = "abc123";
        String originalUrl = "https://example.com/test";
        when(urlService.getOriginalUrl(shortKey)).thenReturn(originalUrl);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/url/redirect/" + shortKey))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.header().string("Location", originalUrl));
    }

    /**
     * 测试短网址访问API - 短网址不存在
     */
    @Test
    void testRedirectUrl_NotFound() throws Exception {
        // Given
        String nonExistentKey = "nonexistent";
        when(urlService.getOriginalUrl(nonExistentKey))
                .thenThrow(new RuntimeException("短网址不存在"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/url/redirect/" + nonExistentKey))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    /**
     * 测试获取短网址统计API - 成功场景
     */
    @Test
    void testGetUrlStats_Success() throws Exception {
        // Given
        String shortKey = "abc123";
        UrlService.UrlStats stats = new UrlService.UrlStats();
        stats.setShortKey(shortKey);
        stats.setOriginalUrl("https://example.com/test");
        stats.setTitle("测试网址");
        stats.setTotalClicks(100L);
        stats.setTodayClicks(25L);
        stats.setStatus(1);

        when(urlService.getUrlStats(shortKey)).thenReturn(stats);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/url/stats/" + shortKey))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ResponseStatus.SUCCESS.getCode()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.shortKey").value(shortKey))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalClicks").value(100L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.todayClicks").value(25L));
    }

    /**
     * 测试获取短网址统计API - 短网址不存在
     */
    @Test
    void testGetUrlStats_NotFound() throws Exception {
        // Given
        String nonExistentKey = "nonexistent";
        when(urlService.getUrlStats(nonExistentKey))
                .thenThrow(new RuntimeException("短网址不存在"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/url/stats/" + nonExistentKey))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    /**
     * 测试短网址列表API - 成功场景
     */
    @Test
    void testGetUrlList_Success() throws Exception {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/url/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "test"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ResponseStatus.SUCCESS.getCode()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists());
    }

    /**
     * 测试删除短网址API - 成功场景
     */
    @Test
    void testDeleteShortUrl_Success() throws Exception {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/url/delete/abc123"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ResponseStatus.SUCCESS.getCode()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("删除成功"));
    }

    /**
     * 测试删除短网址API - 短网址不存在
     */
    @Test
    void testDeleteShortUrl_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/url/delete/nonexistent"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    /**
     * 测试更新短网址状态API - 成功场景
     */
    @Test
    void testUpdateUrlStatus_Success() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("status", 0); // 禁用状态

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/url/status/abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ResponseStatus.SUCCESS.getCode()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("状态更新成功"));
    }

    /**
     * 测试更新短网址状态API - 无效状态值
     */
    @Test
    void testUpdateUrlStatus_InvalidStatus() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("status", 99); // 无效状态

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/url/status/abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}