package com.example.shorturl.service;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.common.security.JwtUtils;
import com.example.shorturl.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务类
 * <p>
 * 模块职责：
 * - 处理用户登录认证
 * - 生成和管理JWT Token
 * - 提供Token验证和刷新
 * - 管理用户会话信息
 * <p>
 * 核心功能：
 * - 用户登录认证
 * - JWT Token生成
 * - Token验证和刷新
 * - 会话管理
 * <p>
 * 依赖关系：
 * - 被AuthController使用
 * - 依赖UserService进行用户验证
 * - 依赖JwtUtils进行Token操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    private final JwtUtils jwtUtils;

    private final OnlineUserService onlineUserService;

    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(String username, String password) {
        log.info("用户登录请求: username={}", username);

        // 验证用户凭据
        User user = userService.authenticateUser(username, password);

        if (user == null) {
            throw new BusinessException(ResponseStatus.PASSWORD_ERROR);
        }

        // 生成JWT Token
        String token = jwtUtils.generateToken(user);

        // 生成Refresh Token（简化实现，实际可以使用不同的过期时间）
        String refreshToken = jwtUtils.generateToken(user);

        // 记录登录时间
        user.setLastLoginTime(LocalDateTime.now());

        log.info("用户登录成功: username={}, userId={}", username, user.getId());

        onlineUserService.markUserOnline(user.getId());

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userInfo(convertToUserInfo(user))
                .expiresIn(jwtUtils.getTokenRemainingTime(token))
                .build();
    }

    /**
     * 用户注册
     */
    @Transactional
    public RegisterResponse register(String username, String password, String email) {
        log.info("用户注册请求: username={}", username);

        // 注册新用户
        User user = userService.registerUser(username, password, email, "USER");

        // 生成JWT Token
        String token = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateToken(user);

        log.info("用户注册成功: username={}, userId={}", username, user.getId());

        return RegisterResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userInfo(convertToUserInfo(user))
                .expiresIn(jwtUtils.getTokenRemainingTime(token))
                .build();
    }

    /**
     * 刷新Token
     */
    @Transactional(readOnly = true)
    public TokenRefreshResponse refreshToken(String refreshToken) {
        log.debug("Token刷新请求");

        // 验证Refresh Token
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ResponseStatus.UNAUTHORIZED.getCode(), "Refresh Token无效或已过期");
        }

        // 提取用户信息
        String username = jwtUtils.extractUsername(refreshToken);
        Long userId = jwtUtils.extractUserId(refreshToken);

        // 验证用户是否存在
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResponseStatus.USER_DISABLED);
        }

        // 生成新的Token
        String newToken = jwtUtils.generateToken(user);
        String newRefreshToken = jwtUtils.generateToken(user);

        log.info("Token刷新成功: userId={}", userId);

        return TokenRefreshResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtUtils.getTokenRemainingTime(newToken))
                .build();
    }

    /**
     * 用户登出
     */
    @Transactional
    public void logout(String authorizationHeader) {
        Long userId = extractUserIdFromAuthorizationHeader(authorizationHeader);
        log.info("用户登出: userId={}", userId);

        onlineUserService.markUserOffline(userId);

        // TODO: 可以实现Token黑名单机制
        // 将Token添加到Redis黑名单，防止继续使用
        // redisTemplate.opsForValue().set("blacklist:" + token, "1", 24, TimeUnit.HOURS);

        // 当前实现：前端清除Token即可
        log.info("用户登出成功: userId={}", userId);
    }

    private Long extractUserIdFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        String token = authorizationHeader;
        if (authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }

        try {
            return jwtUtils.extractUserId(token);
        } catch (Exception e) {
            log.warn("解析登出用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证Token有效性
     */
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            // 验证Token格式和过期时间
            if (!jwtUtils.validateToken(token)) {
                return false;
            }

            // 提取用户信息
            Long userId = jwtUtils.extractUserId(token);
            String username = jwtUtils.extractUsername(token);

            // 验证用户是否存在且可用
            User user = userService.getUserById(userId);
            if (user == null || !username.equals(user.getUsername())) {
                return false;
            }

            // 检查用户状态
            if (user.getStatus() != null && user.getStatus() == 0) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前用户信息
     */
    @Transactional(readOnly = true)
    public UserInfo getCurrentUser(String token) {
        if (!validateToken(token)) {
            throw new BusinessException(ResponseStatus.UNAUTHORIZED);
        }

        Long userId = jwtUtils.extractUserId(token);
        User user = userService.getUserById(userId);

        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }

        return convertToUserInfo(user);
    }

    /**
     * 将User实体转换为UserInfo DTO
     */
    private UserInfo convertToUserInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .createdTime(user.getCreatedTime())
                .build();
    }

    /**
     * 登录响应DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class LoginResponse {
        private String token;
        private String refreshToken;
        private UserInfo userInfo;
        private Long expiresIn; // Token过期时间（毫秒）
    }

    /**
     * 注册响应DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class RegisterResponse {
        private String token;
        private String refreshToken;
        private UserInfo userInfo;
        private Long expiresIn; // Token过期时间（毫秒）
    }

    /**
     * Token刷新响应DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class TokenRefreshResponse {
        private String token;
        private String refreshToken;
        private Long expiresIn; // Token过期时间（毫秒）
    }

    /**
     * 用户信息DTO
     */
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String role;
        private Integer status;
        private LocalDateTime lastLoginTime;
        private LocalDateTime createdTime;
    }
}
