package com.example.shorturl.controller;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.service.UrlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlService urlService;

    @Test
    void testCreateShortUrlSuccess() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("originalUrl", "https://example.com/very/long/url");
        request.put("title", "测试网址");
        request.put("expiredTime", LocalDateTime.now().plusDays(30));

        when(urlService.createShortUrl(anyString(), anyString(), any(LocalDateTime.class))).thenReturn("abc123");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseStatus.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.shortKey").value("abc123"))
                .andExpect(jsonPath("$.data.shortUrl").exists());
    }

    @Test
    void testCreateShortUrlValidationError() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("originalUrl", "");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseStatus.BAD_REQUEST.getCode()));
    }

    @Test
    void testGetUrlStatsSuccess() throws Exception {
        UrlService.UrlStats stats = new UrlService.UrlStats();
        stats.setShortKey("abc123");
        stats.setOriginalUrl("https://example.com/test");
        stats.setTitle("测试网址");
        stats.setTotalClicks(100L);
        stats.setTodayClicks(25L);
        stats.setStatus(1);

        when(urlService.getUrlStats("abc123")).thenReturn(stats);

        mockMvc.perform(get("/api/stats/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseStatus.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.shortKey").value("abc123"))
                .andExpect(jsonPath("$.data.totalClicks").value(100L));
    }

    @Test
    void testGetUrlStatsNotFound() throws Exception {
        when(urlService.getUrlStats("missing"))
                .thenThrow(new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST));

        mockMvc.perform(get("/api/stats/missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseStatus.SHORT_URL_NOT_EXIST.getCode()));
    }

    @Test
    void testRedirectSuccess() throws Exception {
        when(urlService.getOriginalUrl("abc123")).thenReturn("https://example.com/test");

        mockMvc.perform(get("/abc123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://example.com/test"));
    }

    @Test
    void testRedirectNotFound() throws Exception {
        when(urlService.getOriginalUrl("missing"))
                .thenThrow(new BusinessException(ResponseStatus.SHORT_URL_NOT_EXIST));

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound());
    }
}
