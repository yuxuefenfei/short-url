package com.example.shorturl.controller;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.service.UrlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 短网址控制器
 * <p>
 * 模块职责：
 * - 提供短网址生成和管理API
 * - 处理短网址相关的HTTP请求
 * - 参数验证和异常处理
 * <p>
 * API接口：
 * - POST /api/shorten: 创建短网址
 * - GET /api/stats/{shortKey}: 获取统计信息
 * <p>
 * 安全考虑：
 * - 参数验证防止恶意输入
 * - URL格式验证
 * - 长度限制
 * <p>
 * 依赖关系：
 * - 被前端调用
 * - 使用UrlService处理业务逻辑
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @Value("${short-url.domain:https://short.ly}")
    private String shortUrlDomain;

    /**
     * 创建短网址
     *
     * @param request 创建请求参数
     * @return 短网址信息
     */
    @PostMapping("/shorten")
    public ApiResponse<ShortUrlResult> createShortUrl(@Valid @RequestBody ShortUrlRequest request) {
        log.info("创建短网址请求: url={}, title={}", request.getOriginalUrl(), request.getTitle());

        try {
            String shortKey = urlService.createShortUrl(
                    request.getOriginalUrl(),
                    request.getTitle(),
                    request.getExpiredTime()
            );

            ShortUrlResult result = new ShortUrlResult();
            result.setShortUrl(shortUrlDomain + "/" + shortKey);
            result.setShortKey(shortKey);
            result.setOriginalUrl(request.getOriginalUrl());
            result.setTitle(request.getTitle());
            result.setCreatedTime(LocalDateTime.now());

            log.info("短网址创建成功: key={}", shortKey);

            return ApiResponse.success("短网址生成成功", result);

        } catch (BusinessException e) {
            log.warn("短网址创建失败: code={}, message={}", e.getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("短网址创建异常: ", e);
            throw new BusinessException(ResponseStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取短网址统计信息
     *
     * @param shortKey 短网址key
     * @return 统计信息
     */
    @GetMapping("/stats/{shortKey}")
    public ApiResponse<UrlService.UrlStats> getUrlStats(@PathVariable String shortKey) {
        log.info("查询短网址统计: key={}", shortKey);

        try {
            UrlService.UrlStats stats = urlService.getUrlStats(shortKey);
            return ApiResponse.success("查询成功", stats);

        } catch (BusinessException e) {
            log.warn("短网址统计查询失败: key={}, error={}", shortKey, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("短网址统计查询异常: key={}", shortKey, e);
            throw new BusinessException(ResponseStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 短网址创建请求DTO
     */
    @Data
    public static class ShortUrlRequest {
        @NotBlank(message = "原始URL不能为空")
        @Size(max = 2048, message = "URL长度不能超过2048个字符")
        private String originalUrl;

        @Size(max = 255, message = "标题长度不能超过255个字符")
        private String title;

        private LocalDateTime expiredTime;
    }

    /**
     * 短网址创建结果DTO
     */
    @Data
    public static class ShortUrlResult {
        private String shortUrl;
        private String shortKey;
        private String originalUrl;
        private String title;
        private LocalDateTime createdTime;
    }
}
