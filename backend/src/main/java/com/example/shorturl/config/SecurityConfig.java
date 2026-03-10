package com.example.shorturl.config;

import com.example.shorturl.common.security.JwtAuthenticationEntryPoint;
import com.example.shorturl.common.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 * <p>
 * 模块职责：
 * - 配置Web安全策略
 * - 设置认证和授权规则
 * - 配置JWT过滤器
 * - 配置CORS跨域
 * <p>
 * 安全特性：
 * - 无状态会话管理
 * - JWT认证
 * - 基于角色的访问控制
 * - CSRF防护
 * - CORS配置
 * <p>
 * 依赖关系：
 * - 使用JwtAuthenticationFilter进行JWT验证
 * - 与PasswordConfig配合使用
 * - 集成UserDetailsService
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（使用JWT时不需要）
                .csrf(AbstractHttpConfigurer::disable)

                // 配置异常处理
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // 配置会话管理（无状态）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 配置HTTP请求授权
                .authorizeHttpRequests(auth -> auth
                        // 公开访问的接口
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/shorten",
                                "/api/stats/**",
                                "/{shortKey:^[\\w]+$}"
                        ).permitAll()

                        // 管理接口需要ADMIN角色
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )

                // 配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 添加JWT过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 配置CORS跨域
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:5173",  // 前端开发服务器
                "http://localhost:3000",  // 备用端口
                "https://short.ly",        // 生产环境域名
                "https://*.short.ly"      // 子域名
        ));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"
        ));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "User-Agent",
                "DNT",
                "Cache-Control",
                "X-Mx-ReqToken",
                "Keep-Alive",
                "X-Requested-With",
                "If-Modified-Since",
                "X-CustomHeader",
                "X-Request-ID"
        ));

        // 允许携带凭证
        configuration.setAllowCredentials(true);

        // 预检请求缓存时间
        configuration.setMaxAge(3600L);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Request-ID",
                "X-Response-Time"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}