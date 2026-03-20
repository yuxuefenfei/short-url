package com.example.shorturl.common.security;

import com.example.shorturl.common.redis.RedisKeyConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * CSRF防护过滤器
 * <p>
 * 模块职责：
 * - 防止跨站请求伪造(CSRF)攻击
 * - 生成和验证CSRF Token
 * - 保护敏感操作
 * - 支持前后端分离架构
 * <p>
 * 核心功能：
 * - CSRF Token生成
 * - Token验证
 * - Token存储管理
 * - 安全策略配置
 * <p>
 * 依赖关系：
 * - 在Spring Security之后执行
 * - 依赖Redis存储Token
 */
@Slf4j
@Component
public class CsrfFilter extends OncePerRequestFilter {

    // Redis键前缀
    private static final String CSRF_TOKEN_KEY_PREFIX = RedisKeyConstants.CSRF_TOKEN_KEY_PREFIX;
    private static final String CSRF_USER_TOKENS_KEY_PREFIX = RedisKeyConstants.CSRF_USER_TOKENS_KEY_PREFIX;
    // Token配置
    private static final int TOKEN_LENGTH = 32; // Token长度
    private static final int TOKEN_EXPIRE_MINUTES = 30; // Token过期时间(分钟)
    private static final int MAX_TOKENS_PER_USER = 10; // 每个用户最大Token数
    // 需要CSRF保护的HTTP方法
    private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");
    // 排除检查的路径
    private static final String[] EXCLUDED_PATHS = {
            "/health", "/metrics", "/actuator", "/static", "/assets",
            "/api/auth/login", "/api/auth/register", "/api/url/redirect",
            "/api/shorten"
    };
    // 从请求中获取Token的位置
    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";
    private static final String CSRF_PARAM_NAME = "_csrf";
    private static final String CSRF_COOKIE_NAME = "CSRF-TOKEN";
    private final SecureRandom secureRandom = new SecureRandom();
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String requestURI = request.getRequestURI();
            String method = request.getMethod();

            // 检查是否排除路径
            if (shouldExcludePath(requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 检查是否需要CSRF保护
            if (!PROTECTED_METHODS.contains(method)) {
                // 对于GET请求，可以生成新的Token
                if ("GET".equals(method)) {
                    generateAndSetToken(request, response);
                }
                filterChain.doFilter(request, response);
                return;
            }

            // 无状态API（Bearer Token鉴权）不需要CSRF保护
            if (shouldSkipCsrfForStatelessApi(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 验证CSRF Token
            boolean isValid = validateCsrfToken(request);

            if (!isValid) {
                log.warn("CSRF Token验证失败: ip={}, url={}, method={}",
                        getClientIp(request), requestURI, method);

                // 记录攻击行为
                recordAttackAttempt(request);

                // 返回错误响应
                sendCsrfErrorResponse(response);
                return;
            }

            // 验证成功，生成新的Token
            generateAndSetToken(request, response);

            // 继续处理请求
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("CSRF过滤器执行失败: {}", e.getMessage(), e);
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
     * 判断是否应跳过无状态API的CSRF检查
     */
    private boolean shouldSkipCsrfForStatelessApi(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    /**
     * 获取用户标识
     */
    private String getUserIdentifier(HttpServletRequest request) {
        // 优先使用用户ID
        String userId = (String) request.getAttribute("userId");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // 使用Session ID
        String sessionId = request.getSession().getId();
        if (sessionId != null && !sessionId.isEmpty()) {
            return "session:" + sessionId;
        }

        // 使用IP地址
        return "ip:" + getClientIp(request);
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
     * 生成CSRF Token
     */
    private String generateCsrfToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 生成并设置Token
     */
    private void generateAndSetToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            String userIdentifier = getUserIdentifier(request);
            String token = generateCsrfToken();

            // 存储Token到Redis
            String tokenKey = CSRF_TOKEN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(tokenKey, userIdentifier, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

            // 记录用户的Token
            String userTokensKey = CSRF_USER_TOKENS_KEY_PREFIX + userIdentifier;
            redisTemplate.opsForSet().add(userTokensKey, token);
            redisTemplate.expire(userTokensKey, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

            // 清理过期的用户Token
            cleanupUserTokens(userIdentifier);

            // 设置Token到响应头
            response.setHeader(CSRF_HEADER_NAME, token);

            // 设置Token到Cookie
            setCsrfCookie(response, token);

            log.debug("CSRF Token生成成功: user={}, token={}", userIdentifier, token);

        } catch (Exception e) {
            log.error("生成CSRF Token失败: error={}", e.getMessage());
        }
    }

    /**
     * 设置CSRF Cookie
     */
    private void setCsrfCookie(HttpServletResponse response, String token) {
        try {
            // 构建Set-Cookie头
            String cookieValue = String.format(
                    "%s=%s; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=%d",
                    CSRF_COOKIE_NAME,
                    token,
                    TOKEN_EXPIRE_MINUTES * 60
            );

            response.addHeader("Set-Cookie", cookieValue);
        } catch (Exception e) {
            log.error("设置CSRF Cookie失败: error={}", e.getMessage());
        }
    }

    /**
     * 验证CSRF Token
     */
    private boolean validateCsrfToken(HttpServletRequest request) {
        try {
            String userIdentifier = getUserIdentifier(request);
            String requestToken = getTokenFromRequest(request);

            if (requestToken == null || requestToken.isEmpty()) {
                log.warn("CSRF Token不存在: user={}", userIdentifier);
                return false;
            }

            // 检查Token是否存在且有效
            String tokenKey = CSRF_TOKEN_KEY_PREFIX + requestToken;
            String storedUserIdentifier = redisTemplate.opsForValue().get(tokenKey);

            if (storedUserIdentifier == null) {
                log.warn("CSRF Token无效或已过期: user={}, token={}", userIdentifier, requestToken);
                return false;
            }

            // 检查Token是否属于当前用户
            if (!storedUserIdentifier.equals(userIdentifier)) {
                log.warn("CSRF Token用户不匹配: expected={}, actual={}, token={}",
                        storedUserIdentifier, userIdentifier, requestToken);
                return false;
            }

            // 验证成功，删除已使用的Token（一次性使用）
            redisTemplate.delete(tokenKey);

            // 从用户的Token集合中移除
            String userTokensKey = CSRF_USER_TOKENS_KEY_PREFIX + userIdentifier;
            redisTemplate.opsForSet().remove(userTokensKey, requestToken);

            log.debug("CSRF Token验证成功: user={}, token={}", userIdentifier, requestToken);
            return true;

        } catch (Exception e) {
            log.error("CSRF Token验证失败: error={}", e.getMessage());
            return false;
        }
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 从请求头获取
        String token = request.getHeader(CSRF_HEADER_NAME);
        if (token != null && !token.isEmpty()) {
            return token;
        }

        // 从请求参数获取
        token = request.getParameter(CSRF_PARAM_NAME);
        if (token != null && !token.isEmpty()) {
            return token;
        }

        // 从Cookie获取
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (CSRF_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /**
     * 清理用户的过期Token
     */
    private void cleanupUserTokens(String userIdentifier) {
        try {
            String userTokensKey = CSRF_USER_TOKENS_KEY_PREFIX + userIdentifier;
            Set<String> userTokens = redisTemplate.opsForSet().members(userTokensKey);

            if (userTokens != null && userTokens.size() > MAX_TOKENS_PER_USER) {
                // 转换为列表并排序，保留最新的Token
                java.util.List<String> tokenList = new java.util.ArrayList<>(userTokens);
                if (tokenList.size() > MAX_TOKENS_PER_USER) {
                    // 移除最旧的Token
                    int tokensToRemove = tokenList.size() - MAX_TOKENS_PER_USER;
                    for (int i = 0; i < tokensToRemove; i++) {
                        String oldToken = tokenList.get(i);
                        redisTemplate.opsForSet().remove(userTokensKey, oldToken);
                        redisTemplate.delete(CSRF_TOKEN_KEY_PREFIX + oldToken);
                    }
                }
            }

        } catch (Exception e) {
            log.error("清理用户Token失败: user={}, error={}", userIdentifier, e.getMessage());
        }
    }

    /**
     * 记录攻击行为
     */
    private void recordAttackAttempt(HttpServletRequest request) {
        try {
            log.warn("CSRF攻击尝试记录 - IP: {}, URL: {}, Method: {}, User-Agent: {}",
                    getClientIp(request),
                    request.getRequestURI(),
                    request.getMethod(),
                    request.getHeader("User-Agent"));

        } catch (Exception e) {
            log.error("记录攻击行为失败: error={}", e.getMessage());
        }
    }

    /**
     * 发送CSRF错误响应
     */
    private void sendCsrfErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(403); // Forbidden
        response.setContentType("application/json;charset=UTF-8");

        String jsonResponse = String.format(
                "{\"code\": 403, \"message\": \"CSRF Token验证失败，请刷新页面后重试\", \"data\": null, \"timestamp\": \"%s\", \"requestId\": \"%s\"}",
                System.currentTimeMillis(),
                ""
        );

        response.getWriter().write(jsonResponse);
    }

    /**
     * 生成新的CSRF Token（供前端调用）
     */
    public String generateToken(String userIdentifier) {
        try {
            String token = generateCsrfToken();
            String tokenKey = CSRF_TOKEN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(tokenKey, userIdentifier, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

            String userTokensKey = CSRF_USER_TOKENS_KEY_PREFIX + userIdentifier;
            redisTemplate.opsForSet().add(userTokensKey, token);
            redisTemplate.expire(userTokensKey, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

            cleanupUserTokens(userIdentifier);

            return token;
        } catch (Exception e) {
            log.error("生成CSRF Token失败: user={}, error={}", userIdentifier, e.getMessage());
            return null;
        }
    }

    /**
     * 验证Token（供其他服务调用）
     */
    public boolean validateToken(String token, String userIdentifier) {
        try {
            if (token == null || token.isEmpty() || userIdentifier == null) {
                return false;
            }

            String tokenKey = CSRF_TOKEN_KEY_PREFIX + token;
            String storedUserIdentifier = redisTemplate.opsForValue().get(tokenKey);

            if (storedUserIdentifier == null || !storedUserIdentifier.equals(userIdentifier)) {
                return false;
            }

            // 验证成功后删除Token
            redisTemplate.delete(tokenKey);
            String userTokensKey = CSRF_USER_TOKENS_KEY_PREFIX + userIdentifier;
            redisTemplate.opsForSet().remove(userTokensKey, token);

            return true;
        } catch (Exception e) {
            log.error("验证CSRF Token失败: token={}, user={}, error={}", token, userIdentifier, e.getMessage());
            return false;
        }
    }

    /**
     * 清除用户的所有CSRF Token
     */
    public void clearUserTokens(String userIdentifier) {
        try {
            String userTokensKey = CSRF_USER_TOKENS_KEY_PREFIX + userIdentifier;
            Set<String> userTokens = redisTemplate.opsForSet().members(userTokensKey);

            if (userTokens != null) {
                for (String token : userTokens) {
                    redisTemplate.delete(CSRF_TOKEN_KEY_PREFIX + token);
                }
                redisTemplate.delete(userTokensKey);
            }

            log.info("清除用户CSRF Token: user={}", userIdentifier);
        } catch (Exception e) {
            log.error("清除用户CSRF Token失败: user={}, error={}", userIdentifier, e.getMessage());
        }
    }

    /**
     * 获取用户的Token数量
     */
    public int getUserTokenCount(String userIdentifier) {
        try {
            String userTokensKey = CSRF_USER_TOKENS_KEY_PREFIX + userIdentifier;
            Long count = redisTemplate.opsForSet().size(userTokensKey);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取用户Token数量失败: user={}, error={}", userIdentifier, e.getMessage());
            return 0;
        }
    }
}
