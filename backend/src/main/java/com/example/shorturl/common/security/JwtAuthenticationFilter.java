package com.example.shorturl.common.security;

import com.example.shorturl.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * <p>
 * 模块职责：
 * - 拦截HTTP请求进行JWT验证
 * - 从请求头中提取JWT Token
 * - 验证Token有效性并设置认证信息
 * - 处理认证失败情况
 * <p>
 * 工作流程：
 * 1. 从Authorization头中提取Token
 * 2. 验证Token格式和有效性
 * 3. 加载用户信息
 * 4. 设置SecurityContext
 * <p>
 * 依赖关系：
 * - 被SecurityConfig配置使用
 * - 依赖JwtUtils进行Token验证
 * - 依赖UserDetailsServiceImpl加载用户信息
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 从请求中提取Token
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtUtils.validateToken(jwt)) {
                // 从Token中提取用户名
                String username = jwtUtils.extractUsername(jwt);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 加载用户信息
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 验证Token与用户信息匹配
                    if (jwtUtils.validateToken(jwt, userDetails)) {
                        // 创建认证令牌
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        // 设置认证详情
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        // 设置认证信息到安全上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("用户 {} 认证成功", username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("JWT认证过滤器处理失败: {}", e.getMessage());
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从HTTP请求中提取JWT Token
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // 从Authorization头中提取
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 从请求参数中提取（备选方案）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        // 从Cookie中提取（备选方案）
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 排除登录和公开接口
        String path = request.getRequestURI();
        return path.equals("/api/auth/login") ||
                path.equals("/api/auth/register") ||
                path.equals("/api/auth/check-username") ||
                path.equals("/api/client-errors") ||
                path.startsWith("/api/shorten") ||
                path.startsWith("/api/stats") ||
                path.matches("/[\\w]+");
    }
}
