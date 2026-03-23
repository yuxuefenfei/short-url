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
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * XSS攻击防护过滤器
 * <p>
 * 模块职责：
 * - 检测和阻止跨站脚本攻击(XSS)
 * - 过滤危险的HTML标签和JavaScript代码
 * - 保护用户数据安全
 * - 记录可疑攻击行为
 * <p>
 * 核心功能：
 * - XSS模式检测
 * - HTML标签过滤
 * - JavaScript代码阻止
 * - 安全响应头设置
 * <p>
 * 依赖关系：
 * - 在请求处理早期执行
 * - 与其他安全过滤器配合工作
 */
@Slf4j
@Component
public class XssFilter extends OncePerRequestFilter {

    // XSS检测正则表达式
    private static final Pattern[] XSS_PATTERNS = {
            // 检测<script>标签
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),

            // 检测javascript:协议
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),

            // 检测vbscript:协议
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),

            // 检测data:协议
            Pattern.compile("data:", Pattern.CASE_INSENSITIVE),

            // 检测on事件处理程序
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),

            // 检测eval()函数
            Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),

            // 检测expression()函数
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),

            // 检测innerHTML属性
            Pattern.compile("innerhtml", Pattern.CASE_INSENSITIVE),

            // 检测document.cookie
            Pattern.compile("document\\.cookie", Pattern.CASE_INSENSITIVE),

            // 检测document.write
            Pattern.compile("document\\.write", Pattern.CASE_INSENSITIVE),

            // 检测window.location
            Pattern.compile("window\\.location", Pattern.CASE_INSENSITIVE),

            // 检测危险的HTML标签
            Pattern.compile("<(iframe|frame|embed|object)[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),

            // 检测base标签
            Pattern.compile("<base[^>]*>", Pattern.CASE_INSENSITIVE),

            // 检测meta标签重定向
            Pattern.compile("<meta[^>]*http-equiv\\s*=\\s*[\"']refresh[\"'][^>]*>", Pattern.CASE_INSENSITIVE),

            // 检测CSS表达式
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),

            // 检测CSS url()函数
            Pattern.compile("url\\s*\\([^)]*javascript:", Pattern.CASE_INSENSITIVE),

            // 检测HTML注释中的脚本
            Pattern.compile("<!--.*<script.*-->"),

            // 检测CDATA中的脚本
            Pattern.compile("<!\\[CDATA\\[.*<script.*\\]\\]>"),

            // 检测危险的实体编码
            Pattern.compile("&#x?[0-9a-f]+", Pattern.CASE_INSENSITIVE),

            // 检测危险的HTML属性
            Pattern.compile("(href|src|action|xlink:href)\\s*=\\s*[\"'][^'\"]*javascript:", Pattern.CASE_INSENSITIVE)
    };

    // 需要严格检查的参数名
    private static final String[] STRICT_CHECK_PARAMS = {
            "content", "description", "comment", "message", "text", "html",
            "title", "name", "value", "data", "input", "output"
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

            // 设置安全响应头
            setSecurityHeaders(response);

            // 检查请求参数中的XSS
            boolean isXssAttack = checkRequestParameters(request);

            if (isXssAttack) {
                log.warn("检测到XSS攻击: ip={}, url={}, method={}",
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
                    log.warn("检测到请求体XSS攻击: ip={}, url={}",
                            getClientIp(request), requestURI);

                    recordAttackAttempt(request);
                    sendSecurityResponse(response);
                    return;
                }
            }

            // 继续处理请求
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("XSS过滤器执行失败: {}", e.getMessage(), e);
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
     * 设置安全响应头
     */
    private void setSecurityHeaders(HttpServletResponse response) {
        // 内容安全策略
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; frame-ancestors 'none'; form-action 'self';");

        // XSS防护头
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // 防止MIME类型嗅探
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 点击劫持防护
        response.setHeader("X-Frame-Options", "DENY");

        // 强制HTTPS
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // 推荐的安全头
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
    }

    /**
     * 检查请求参数
     */
    private boolean checkRequestParameters(HttpServletRequest request) {
        java.util.Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);

            if (paramValues != null) {
                for (String paramValue : paramValues) {
                    if (isXssAttack(paramValue, paramName)) {
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
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                // 对于JSON请求，可以添加特殊的XSS检测逻辑
                // 注意：读取请求体会影响后续处理，需要谨慎实现
                return false;
            }

            // 检查表单数据
            java.util.Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);

                if (paramValues != null) {
                    for (String paramValue : paramValues) {
                        if (isXssAttack(paramValue, paramName)) {
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
     * 检测XSS攻击
     */
    private boolean isXssAttack(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // 对严格检查参数进行更严格的检测
        boolean isStrictParam = false;
        for (String strictParam : STRICT_CHECK_PARAMS) {
            if (strictParam.equalsIgnoreCase(paramName)) {
                isStrictParam = true;
                break;
            }
        }

        // 检查XSS模式
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                // 对于严格检查参数，直接阻止
                if (isStrictParam) {
                    return true;
                }

                // 对于非严格参数，检查是否有多个XSS特征
                int xssFeatureCount = countXssFeatures(value);
                if (xssFeatureCount >= 2) {
                    return true;
                }
            }
        }

        // 检查危险的字符组合
        if (containsMaliciousCharacterCombination(value)) {
            return true;
        }

        return false;
    }

    /**
     * 计算XSS特征数量
     */
    private int countXssFeatures(String value) {
        int count = 0;

        String[] xssKeywords = {"script", "javascript", "vbscript", "expression",
                "eval", "document.cookie", "window.location",
                "innerHTML", "iframe", "frame"};

        String lowerValue = value.toLowerCase(Locale.ROOT);
        for (String keyword : xssKeywords) {
            if (lowerValue.contains(keyword)) {
                count++;
            }
        }

        return count;
    }

    /**
     * 检查恶意字符组合
     */
    private boolean containsMaliciousCharacterCombination(String value) {
        // 检查常见的XSS payload
        String[] maliciousPayloads = {
                "<script>",
                "</script>",
                "javascript:",
                "onload=",
                "onerror=",
                "onclick=",
                "onmouseover=",
                "<img src=x onerror=",
                "<svg onload=",
                "<iframe src=",
                "document.cookie",
                "document.write",
                "window.location",
                "eval(",
                "expression(",
                "alert(",
                "prompt(",
                "confirm("
        };

        String lowerValue = value.toLowerCase(Locale.ROOT);
        for (String payload : maliciousPayloads) {
            if (lowerValue.contains(payload)) {
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
            log.warn("XSS攻击尝试记录 - IP: {}, URL: {}, Method: {}, User-Agent: {}",
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
                "{\"code\": 400, \"message\": \"请求包含潜在的XSS攻击内容，已被阻止\", \"data\": null, \"timestamp\": \"%s\", \"requestId\": \"%s\"}",
                System.currentTimeMillis(),
                ""
        );

        response.getWriter().write(jsonResponse);
    }

    /**
     * XSS防护工具方法
     */
    public static class XssUtils {

        /**
         * HTML编码
         */
        public static String htmlEncode(String input) {
            if (input == null) {
                return null;
            }

            return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;")
                    .replace("/", "&#x2F;");
        }

        /**
         * JavaScript编码
         */
        public static String jsEncode(String input) {
            if (input == null) {
                return null;
            }

            return input.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\"", "\\\"")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("/", "\\/");
        }

        /**
         * URL编码
         */
        public static String urlEncode(String input) {
            if (input == null) {
                return null;
            }

            try {
                return java.net.URLEncoder.encode(input, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return input;
            }
        }

        /**
         * 检查字符串是否包含XSS攻击
         */
        public static boolean containsXss(String input) {
            if (input == null || input.trim().isEmpty()) {
                return false;
            }

            XssFilter filter = new XssFilter();
            return filter.isXssAttack(input, "input");
        }

        /**
         * 清理HTML内容
         */
        public static String sanitizeHtml(String html) {
            if (html == null) {
                return null;
            }

            // 简单的HTML清理，实际项目中建议使用专门的HTML清理库如Jsoup
            return htmlEncode(html);
        }
    }
}
