package com.example.shorturl.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * SQL注入防护过滤器
 * <p>
 * 模块职责：
 * - 检测和阻止SQL注入攻击
 * - 分析请求参数中的SQL关键字
 * - 保护系统数据库安全
 * - 记录可疑攻击行为
 * <p>
 * 核心功能：
 * - SQL关键字检测
 * - 特殊字符过滤
 * - 攻击行为记录
 * - 安全响应返回
 * <p>
 * 依赖关系：
 * - 在业务处理之前执行
 * - 与其他安全过滤器配合
 */
@Slf4j
@Component
public class SqlInjectionFilter extends OncePerRequestFilter {

    // SQL注入检测正则表达式
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
            // 检测SQL关键字
            Pattern.compile("(?i)(union|select|insert|update|delete|drop|alter|create|exec|execute)"),

            // 检测SQL注释
            Pattern.compile("(--|#|/\\*|\\*/)"),

            // 检测SQL特殊字符
            Pattern.compile("('|;|\\|\\|&&|\\$\\(|`)"),

            // 检测SQL函数
            Pattern.compile("(?i)(sleep|benchmark|pg_sleep|waitfor|delay)"),

            // 检测SQL系统表
            Pattern.compile("(?i)(information_schema|mysql\\.user|sys\\.|pg_catalog)"),

            // 检测SQL联合查询
            Pattern.compile("(?i)union[\\s\\n\\r]*select"),

            // 检测SQL盲注特征
            Pattern.compile("(?i)(and|or)[\\s\\n\\r]*\\d+[\\s\\n\\r]*=[\\s\\n\\r]*\\d+"),

            // 检测SQL报错注入
            Pattern.compile("(?i)(extractvalue|updatexml|exp|floor)"),

            // 检测SQL堆叠查询
            Pattern.compile(";[\\s\\n\\r]*(select|insert|update|delete|drop)"),

            // 检测SQL内联注释
            Pattern.compile("/\\*[\\s\\S]*?\\*/")
    };

    // 需要检查的参数名（敏感参数）
    private static final String[] SENSITIVE_PARAMS = {
            "username", "password", "email", "url", "title", "content",
            "search", "query", "keyword", "id", "name", "value"
    };

    // 排除检查的路径
    private static final String[] EXCLUDED_PATHS = {
            "/health", "/metrics", "/actuator", "/static", "/assets"
    };

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String requestURI = request.getRequestURI();

            // 检查是否排除路径
            if (shouldExcludePath(requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 检查请求参数
            boolean isAttack = checkRequestParameters(request);

            if (isAttack) {
                log.warn("检测到SQL注入攻击: ip={}, url={}, method={}",
                        getClientIp(request), requestURI, request.getMethod());

                // 记录攻击行为
                recordAttackAttempt(request);

                // 返回安全响应
                sendSecurityResponse(response);
                return;
            }

            // 检查请求体（仅POST/PUT请求）
            if ("POST".equalsIgnoreCase(request.getMethod()) ||
                    "PUT".equalsIgnoreCase(request.getMethod())) {

                boolean isBodyAttack = checkRequestBody(request);
                if (isBodyAttack) {
                    log.warn("检测到请求体SQL注入攻击: ip={}, url={}",
                            getClientIp(request), requestURI);

                    recordAttackAttempt(request);
                    sendSecurityResponse(response);
                    return;
                }
            }

            // 继续处理请求
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("SQL注入过滤器执行失败: {}", e.getMessage(), e);
            // 过滤器异常时继续处理
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 检查是否排除路径
     */
    private boolean shouldExcludePath(String requestURI) {
        for (String excludedPath : EXCLUDED_PATHS) {
            if (requestURI.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查请求参数
     */
    private boolean checkRequestParameters(HttpServletRequest request) {
        // 检查查询参数
        java.util.Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);

            if (paramValues != null) {
                for (String paramValue : paramValues) {
                    if (isSqlInjection(paramValue, paramName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查请求体
     */
    private boolean checkRequestBody(HttpServletRequest request) {
        try {
            // 对于JSON请求，需要特殊处理
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                // 这里可以添加JSON内容的SQL注入检测
                // 由于需要读取请求体，可能会影响后续处理，需要谨慎实现
                return false;
            }

            // 检查表单数据
            java.util.Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);

                if (paramValues != null) {
                    for (String paramValue : paramValues) {
                        if (isSqlInjection(paramValue, paramName)) {
                            return true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("检查请求体失败: error={}", e.getMessage());
        }

        return false;
    }

    /**
     * 检测SQL注入
     */
    private boolean isSqlInjection(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // 对敏感参数进行更严格的检查
        boolean isSensitiveParam = false;
        for (String sensitiveParam : SENSITIVE_PARAMS) {
            if (sensitiveParam.equalsIgnoreCase(paramName)) {
                isSensitiveParam = true;
                break;
            }
        }

        String lowerValue = value.toLowerCase();

        // 检查SQL注入模式
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(lowerValue).find()) {
                // 对于敏感参数，更严格的检测
                if (isSensitiveParam) {
                    return true;
                }

                // 对于非敏感参数，检查是否有多个SQL特征
                int sqlFeatureCount = countSqlFeatures(lowerValue);
                if (sqlFeatureCount >= 2) {
                    return true;
                }
            }
        }

        // 检查特殊字符组合
        if (containsMaliciousCharacterCombination(value)) {
            return true;
        }

        return false;
    }

    /**
     * 计算SQL特征数量
     */
    private int countSqlFeatures(String value) {
        int count = 0;

        String[] sqlKeywords = {"union", "select", "insert", "update", "delete", "drop",
                "alter", "create", "exec", "execute", "truncate"};

        for (String keyword : sqlKeywords) {
            if (value.contains(keyword)) {
                count++;
            }
        }

        return count;
    }

    /**
     * 检查恶意字符组合
     */
    private boolean containsMaliciousCharacterCombination(String value) {
        // 检查常见的SQL注入字符组合
        String[] maliciousCombinations = {
                "'or'1'='1",
                "'or 1=1--",
                "'or 1=1#",
                "'or 1=1/*",
                "')or('1'='1",
                "')or'1'='1--",
                "')or'1'='1#",
                "')or'1'='1/*",
                "1'or'1'='1",
                "1'or 1=1--",
                "1'or 1=1#",
                "1'or 1=1/*"
        };

        String lowerValue = value.toLowerCase();
        for (String combination : maliciousCombinations) {
            if (lowerValue.contains(combination)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 记录攻击行为
     */
    private void recordAttackAttempt(HttpServletRequest request) {
        try {
            // TODO: 可以记录到数据库或专门的日志系统
            // 这里简单记录到应用日志
            log.warn("SQL注入攻击尝试记录 - IP: {}, URL: {}, Method: {}, User-Agent: {}",
                    getClientIp(request),
                    request.getRequestURI(),
                    request.getMethod(),
                    request.getHeader("User-Agent"));

        } catch (Exception e) {
            log.error("记录攻击行为失败: error={}", e.getMessage());
        }
    }

    /**
     * 发送安全响应
     */
    private void sendSecurityResponse(HttpServletResponse response) throws IOException {
        response.setStatus(400); // Bad Request
        response.setContentType("application/json;charset=UTF-8");

        String jsonResponse = String.format(
                "{\"code\": 400, \"message\": \"请求包含不安全的内容，已被阻止\", \"data\": null, \"timestamp\": \"%s\", \"requestId\": \"%s\"}",
                System.currentTimeMillis(),
                ""
        );

        response.getWriter().write(jsonResponse);
    }

    /**
     * SQL注入检测工具方法
     */
    public static class SqlInjectionUtils {

        /**
         * 清理SQL注入字符
         */
        public static String sanitizeInput(String input) {
            if (input == null) {
                return null;
            }

            // 移除或转义危险字符
            return input.replace("'", "''")
                    .replace(";", "")
                    .replace("--", "")
                    .replace("/*", "")
                    .replace("*/", "")
                    .replace("\\", "\\\\");
        }

        /**
         * 检查字符串是否包含SQL注入
         */
        public static boolean containsSqlInjection(String input) {
            if (input == null || input.trim().isEmpty()) {
                return false;
            }

            SqlInjectionFilter filter = new SqlInjectionFilter();
            return filter.isSqlInjection(input, "input");
        }

        /**
         * 安全的字符串匹配（防止SQL注入）
         */
        public static boolean safeStringMatch(String input, String pattern) {
            if (containsSqlInjection(input) || containsSqlInjection(pattern)) {
                return false;
            }

            try {
                return input != null && input.matches(pattern);
            } catch (Exception e) {
                return false;
            }
        }
    }
}