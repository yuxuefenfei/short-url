package com.example.shorturl.controller;

import com.example.shorturl.common.annotation.RequiresLog;
import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.common.response.PageResult;
import com.example.shorturl.model.entity.User;
import com.example.shorturl.service.UrlService;
import com.example.shorturl.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private UrlService urlService;

    @RequiresLog(type = "QUERY", module = "USER_MANAGEMENT", description = "查询用户列表")
    @GetMapping("/users")
    public ApiResponse<PageResult<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {

        PageResult<User> result = PageResult.of(
                userService.getUserList(page, size, keyword, role, status),
                userService.getUserCount(keyword, role, status),
                page,
                size
        );
        return ApiResponse.success(result);
    }

    @RequiresLog(type = "CREATE", module = "USER_MANAGEMENT", description = "创建用户")
    @PostMapping("/users")
    public ApiResponse<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getRole()
        );
        return ApiResponse.success("用户创建成功", user);
    }

    @RequiresLog(type = "QUERY", module = "USER_MANAGEMENT", description = "查询用户详情")
    @GetMapping("/users/{userId}")
    public ApiResponse<User> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.success(userService.getUserById(userId));
    }

    @RequiresLog(type = "UPDATE", module = "USER_MANAGEMENT", description = "更新用户状态")
    @PutMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable @NotNull Long userId,
                                              @Valid @RequestBody UserStatusRequest request) {
        userService.updateUserStatus(userId, request.getStatus());
        return ApiResponse.success();
    }

    @RequiresLog(type = "UPDATE", module = "USER_MANAGEMENT", description = "更新用户信息")
    @PutMapping("/users/{userId}")
    public ApiResponse<User> updateUserInfo(@PathVariable @NotNull Long userId,
                                            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateUserInfo(userId, request.getEmail(), request.getRole()));
    }

    @RequiresLog(type = "UPDATE", module = "USER_MANAGEMENT", description = "重置用户密码")
    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetUserPassword(@PathVariable Long userId) {
        String password = "Reset@" + UUID.randomUUID().toString().substring(0, 8);
        userService.resetPassword(userId, password);

        ResetPasswordResponse response = new ResetPasswordResponse();
        response.setTemporaryPassword(password);
        return ApiResponse.success("密码重置成功", response);
    }

    @RequiresLog(type = "DELETE", module = "USER_MANAGEMENT", description = "删除用户")
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable @NotNull Long userId) {
        userService.updateUserStatus(userId, 0);
        return ApiResponse.success();
    }

    @RequiresLog(type = "QUERY", module = "SYSTEM_MONITOR", description = "查询系统统计")
    @GetMapping("/system-stats")
    public ApiResponse<SystemStats> getSystemStats() {
        SystemStats stats = new SystemStats();
        stats.setTotalUsers(userService.getUserCount(null, null, null));
        stats.setOnlineUsers(0L);
        stats.setTotalUrls(urlService.getUrlCount(null, null));
        stats.setTotalClicks(urlService.getTotalClicks());
        stats.setTodayNewUrls(urlService.getTodayNewUrls());
        stats.setTodayClicks(urlService.getTodayClicks());
        stats.setSystemStartTime(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()),
                ZoneId.systemDefault()
        ));
        stats.setVersion("1.0.0");
        return ApiResponse.success(stats);
    }

    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String email;
        private String role;
    }

    @Data
    public static class UpdateUserRequest {
        private String email;
        private String role;
    }

    @Data
    public static class UserStatusRequest {
        private Integer status;
    }

    @Data
    public static class ResetPasswordResponse {
        private String temporaryPassword;
    }

    @Data
    public static class SystemStats {
        private Long totalUsers;
        private Long onlineUsers;
        private Long totalUrls;
        private Long totalClicks;
        private Long todayNewUrls;
        private Long todayClicks;
        private LocalDateTime systemStartTime;
        private String version;
    }
}
