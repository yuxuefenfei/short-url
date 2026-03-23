package com.example.shorturl.controller;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 短网址重定向控制器
 * <p>
 * 模块职责：
 * - 处理短网址访问重定向
 * - 实现HTTP 302重定向
 * - 记录访问日志
 * <p>
 * 核心功能：
 * - 接收短网址key访问请求
 * - 查找对应的原始URL
 * - 执行HTTP 302重定向
 * - 异步记录访问日志
 * <p>
 * 性能优化：
 * - Redis缓存加速查找
 * - 异步日志记录
 * - 最小化响应时间
 * <p>
 * 安全考虑：
 * - 验证短网址key格式
 * - 检查URL状态和过期时间
 * - 防止恶意重定向
 * <p>
 * 依赖关系：
 * - 被Nginx直接调用
 * - 使用UrlService获取原始URL
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;

    /**
     * 短网址重定向
     *
     * @param shortKey 短网址key
     * @param request  HTTP请求
     * @return HTTP 302重定向响应
     */
    @GetMapping("/{shortKey:^[\\w]+$}")
    public ResponseEntity<Void> redirect(@PathVariable String shortKey, HttpServletRequest request) {
        log.debug("短网址重定向请求: key={}, ip={}, userAgent={}",
                shortKey,
                getClientIp(request),
                request.getHeader("User-Agent"));

        try {
            // 获取原始URL
            String originalUrl = urlService.getOriginalUrl(shortKey);

            // 执行HTTP 302重定向
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(originalUrl))
                    .build();

        } catch (BusinessException e) {
            log.warn("短网址重定向失败: key={}, error={}", shortKey, e.getMessage());

            // 根据错误类型返回不同的HTTP状态码
            if (e.getCode() == ResponseStatus.SHORT_URL_NOT_EXIST.getCode()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if (e.getCode() == ResponseStatus.SHORT_URL_EXPIRED.getCode() ||
                    e.getCode() == ResponseStatus.SHORT_URL_DISABLED.getCode()) {
                return ResponseEntity.status(HttpStatus.GONE).build();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

        } catch (Exception e) {
            log.error("短网址重定向异常: key={}", shortKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况（X-Forwarded-For）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
