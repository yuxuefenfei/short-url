package com.example.shorturl.common.security;

import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.common.response.ResponseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * JWT认证异常处理器
 * <p>
 * 模块职责：
 * - 处理未认证用户的访问请求
 * - 返回标准的JSON格式错误响应
 * - 记录认证失败日志
 * <p>
 * 使用场景：
 * - 用户未登录访问受保护资源
 * - Token无效或过期
 * - 认证信息缺失
 * <p>
 * 依赖关系：
 * - 被SecurityConfig配置使用
 * - 与ApiResponse配合统一响应格式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        // 记录认证失败日志
        log.warn("认证失败: IP={}, URL={}, Method={}, Error={}",
                getClientIp(request),
                request.getRequestURI(),
                request.getMethod(),
                Objects.requireNonNullElse(authException == null ? null : authException.getMessage(), "未知错误")
        );

        // 设置响应状态码
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 创建错误响应
        ApiResponse<Object> apiResponse = ApiResponse.error(
                ResponseStatus.UNAUTHORIZED.getCode(),
                "认证失败，请重新登录"
        );

        // 添加额外的错误信息
        if (authException != null) {
            log.debug("认证异常详情: ", authException);
        }

        // 写入响应
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(apiResponse));
            writer.flush();
        } catch (Exception e) {
            log.error("写入认证失败响应时发生错误: {}", e.getMessage());

            // 如果JSON写入失败，返回简单的文本响应
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            try (PrintWriter writer = response.getWriter()) {
                writer.write("认证失败，请重新登录");
                writer.flush();
            }
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
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}