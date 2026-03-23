package com.example.shorturl.controller;

import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.model.dto.LoginRequest;
import com.example.shorturl.model.dto.RegisterRequest;
import com.example.shorturl.service.AuthService;
import com.example.shorturl.service.OnlineUserService;
import com.example.shorturl.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * <p>
 * 模块职责：
 * - 处理用户登录和注册请求
 * - 提供Token刷新功能
 * - 管理用户认证状态
 * <p>
 * 接口安全：
 * - 登录和注册接口公开访问
 * - 使用参数验证确保数据完整性
 * - 返回标准化的认证响应
 * <p>
 * 依赖关系：
 * - 被前端登录页面调用
 * - 依赖AuthService处理认证逻辑
 * - 与SecurityConfig配合实现安全控制
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private OnlineUserService onlineUserService;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求体
     * @return 登录结果，包含Token和用户信息
     */
    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        log.info("用户登录请求: username={}", loginRequest.getUsername());

        try {
            AuthService.LoginResponse response = authService.login(
                    loginRequest.getUsername(),
                    loginRequest.getPassword());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("用户登录失败: username={}, error={}", loginRequest.getUsername(), e.getMessage());
            throw e;
        }
    }

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求体
     * @return 注册结果，包含Token和用户信息
     */
    @PostMapping("/register")
    public ApiResponse<AuthService.RegisterResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest) {

        log.info("用户注册请求: username={}", registerRequest.getUsername());

        try {
            AuthService.RegisterResponse response = authService.register(
                    registerRequest.getUsername(),
                    registerRequest.getPassword(),
                    registerRequest.getEmail());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("用户注册失败: username={}, error={}", registerRequest.getUsername(), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/check-username")
    public ApiResponse<UsernameCheckResponse> checkUsername(@RequestParam("username") String username) {
        boolean exists = userService.usernameExists(username);
        return ApiResponse.success(new UsernameCheckResponse(exists));
    }

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新Token
     * @return 新的Token对
     */
    @PostMapping("/refresh-token")
    public ApiResponse<AuthService.TokenRefreshResponse> refreshToken(
            @RequestHeader("Refresh-Token") String refreshToken) {

        log.debug("Token刷新请求");

        try {
            AuthService.TokenRefreshResponse response = authService.refreshToken(refreshToken);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Token刷新失败: error={}", e.getMessage());
            throw e;
        }
    }

    /**
     * 用户登出
     *
     * @param token 用户Token
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String token) {
        log.info("用户登出请求");

        try {
            authService.logout(token);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("用户登出失败: error={}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证Token有效性
     *
     * @param token 用户Token
     * @return 验证结果
     */
    @GetMapping("/validate-token")
    public ApiResponse<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        log.debug("Token验证请求");

        try {
            boolean isValid = authService.validateToken(token);
            return ApiResponse.success(isValid);
        } catch (Exception e) {
            log.error("Token验证失败: error={}", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取当前用户信息
     *
     * @param token 用户Token
     * @return 用户信息
     */
    @GetMapping("/current-user")
    public ApiResponse<AuthService.UserInfo> getCurrentUser(@RequestHeader("Authorization") String token) {
        log.debug("获取当前用户信息请求");

        try {
            AuthService.UserInfo userInfo = authService.getCurrentUser(token);
            return ApiResponse.success(userInfo);
        } catch (Exception e) {
            log.error("获取当前用户信息失败: error={}", e.getMessage());
            throw e;
        }
    }

    public record UsernameCheckResponse(boolean exists) {
    }
}
