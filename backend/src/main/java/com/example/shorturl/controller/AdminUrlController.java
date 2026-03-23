package com.example.shorturl.controller;

import com.example.shorturl.common.annotation.RequiresLog;
import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.common.response.PageResult;
import com.example.shorturl.model.entity.ShortUrlMapping;
import com.example.shorturl.service.UrlService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUrlController {

    private final UrlService urlService;

    @Value("${short-url.domain:https://short.ly}")
    private String shortUrlDomain;

    @RequiresLog(type = "QUERY", module = "URL_MANAGEMENT", description = "查询短链列表")
    @GetMapping("/urls")
    public ApiResponse<PageResult<AdminUrlView>> getUrlList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        var urlList = urlService.getUrlList(page, size, keyword, status)
                .stream()
                .map(this::toAdminUrlView)
                .toList();

        return ApiResponse.success(PageResult.of(
                urlList,
                urlService.getUrlCount(keyword, status),
                page,
                size
        ));
    }

    @RequiresLog(type = "QUERY", module = "URL_MANAGEMENT", description = "查询短链详情")
    @GetMapping("/urls/{id}")
    public ApiResponse<ShortUrlMapping> getUrlDetail(@PathVariable Long id) {
        return ApiResponse.success(urlService.getUrlById(id));
    }

    @RequiresLog(type = "UPDATE", module = "URL_MANAGEMENT", description = "更新短链信息")
    @PutMapping("/urls/{id}")
    public ApiResponse<ShortUrlMapping> updateUrl(@PathVariable Long id, @RequestBody UpdateUrlRequest request) {
        return ApiResponse.success(urlService.updateUrl(id, request.getTitle(), request.getExpiredTime()));
    }

    @RequiresLog(type = "UPDATE", module = "URL_MANAGEMENT", description = "更新短链状态")
    @PutMapping("/urls/{id}/status")
    public ApiResponse<Void> updateUrlStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        urlService.updateUrlStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    @RequiresLog(type = "DELETE", module = "URL_MANAGEMENT", description = "删除短链")
    @DeleteMapping("/urls/{id}")
    public ApiResponse<Void> deleteUrl(@PathVariable Long id) {
        urlService.deleteUrl(id);
        return ApiResponse.success();
    }

    @RequiresLog(type = "BATCH_UPDATE", module = "URL_MANAGEMENT", description = "批量更新短链状态")
    @PutMapping("/urls/batch-status")
    public ApiResponse<Void> batchUpdateStatus(@RequestParam List<Long> ids, @RequestBody StatusRequest request) {
        ids.forEach(id -> urlService.updateUrlStatus(id, request.getStatus()));
        return ApiResponse.success();
    }

    @RequiresLog(type = "BATCH_DELETE", module = "URL_MANAGEMENT", description = "批量删除短链")
    @DeleteMapping("/urls/batch")
    public ApiResponse<Void> batchDeleteUrls(@RequestBody List<Long> ids) {
        ids.forEach(urlService::deleteUrl);
        return ApiResponse.success();
    }

    @RequiresLog(type = "QUERY", module = "URL_STATISTICS", description = "查询短链统计")
    @GetMapping("/urls/{shortKey}/stats")
    public ApiResponse<UrlService.UrlStats> getUrlStats(@PathVariable String shortKey) {
        return ApiResponse.success(urlService.getUrlStats(shortKey));
    }

    @Data
    public static class StatusRequest {
        private Integer status;
    }

    @Data
    public static class UpdateUrlRequest {
        private String title;
        private LocalDateTime expiredTime;
    }

    @Data
    public static class AdminUrlView {
        private Long id;
        private String shortKey;
        private String shortUrl;
        private String originalUrl;
        private String title;
        private Long clickCount;
        private Integer status;
        private LocalDateTime createdTime;
        private LocalDateTime expiredTime;
        private LocalDateTime updatedTime;
    }

    private AdminUrlView toAdminUrlView(ShortUrlMapping mapping) {
        AdminUrlView view = new AdminUrlView();
        BeanUtils.copyProperties(mapping, view);
        view.setShortUrl(shortUrlDomain + "/" + mapping.getShortKey());
        return view;
    }
}
