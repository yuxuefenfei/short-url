package com.example.shorturl.controller;

import com.example.shorturl.common.annotation.RequiresLog;
import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.common.response.PageResult;
import com.example.shorturl.model.entity.User;
import com.example.shorturl.service.UserService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员控制器
 * <p>
 * 模块职责：
 * - 提供用户管理功能
 * - 处理系统监控请求
 * - 管理管理员操作
 * <p>
 * 安全特性：
 * - 需要ADMIN角色权限
 * - 所有操作记录审计日志
 * - 参数验证和异常处理
 * <p>
 * 依赖关系：
 * - 被前端管理后台调用
 * - 依赖UserService处理用户操作
 * - 与SecurityConfig配合实现权限控制
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户列表
     *
     * @param page    页码
     * @param size    每页数量
     * @param keyword 搜索关键词
     * @return 用户列表分页结果
     */
    @RequiresLog(type = "QUERY", module = "USER_MANAGEMENT", description = "查询用户列表")
    @GetMapping("/users")
    public ApiResponse<PageResult<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {

        log.info("查询用户列表: page={}, size={}, keyword={}", page, size, keyword);

        try {
            List<User> userList = userService.getUserList(page, size, keyword);
            Long total = userService.getUserCount(keyword);

            PageResult<User> result = new PageResult<>(userList, total, page, size);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询用户列表失败: error={}", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详细信息
     */
    @RequiresLog(type = "QUERY", module = "USER_MANAGEMENT", description = "查询用户详情")
    @GetMapping("/users/{userId}")
    public ApiResponse<User> getUserDetail(@PathVariable Long userId) {
        log.info("查询用户详情: userId={}", userId);

        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                return ApiResponse.error(404, "用户不存在");
            }
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("查询用户详情失败: userId={}, error={}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 用户状态 (1:正常, 0:禁用)
     * @return 操作结果
     */
    @RequiresLog(type = "UPDATE", module = "USER_MANAGEMENT", description = "更新用户状态")
    @PutMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {

        log.info("更新用户状态: userId={}, status={}", userId, status);

        try {
            userService.updateUserStatus(userId, status);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("更新用户状态失败: userId={}, status={}, error={}", userId, status, e.getMessage());
            throw e;
        }
    }

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param email  邮箱
     * @param role   角色
     * @return 更新后的用户信息
     */
    @RequiresLog(type = "UPDATE", module = "USER_MANAGEMENT", description = "更新用户信息")
    @PutMapping("/users/{userId}")
    public ApiResponse<User> updateUserInfo(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role) {

        log.info("更新用户信息: userId={}, email={}, role={}", userId, email, role);

        try {
            User user = userService.updateUserInfo(userId, email, role);
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("更新用户信息失败: userId={}, error={}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @RequiresLog(type = "DELETE", module = "USER_MANAGEMENT", description = "删除用户")
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("删除用户: userId={}", userId);

        try {
            // 检查用户是否存在
            User user = userService.getUserById(userId);
            if (user == null) {
                return ApiResponse.error(404, "用户不存在");
            }

            // 不允许删除管理员账户
            if ("ADMIN".equals(user.getRole())) {
                return ApiResponse.error(403, "不能删除管理员账户");
            }

            // 执行删除操作（软删除，更新状态）
            userService.updateUserStatus(userId, 0);

            return ApiResponse.success();
        } catch (Exception e) {
            log.error("删除用户失败: userId={}, error={}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取系统统计信息
     *
     * @return 系统统计数据
     */
    @RequiresLog(type = "QUERY", module = "SYSTEM_MONITOR", description = "查询系统统计")
    @GetMapping("/system-stats")
    public ApiResponse<SystemStats> getSystemStats() {
        log.info("查询系统统计信息");

        try {
            SystemStats stats = SystemStats.builder()
                    .totalUsers(userService.getUserCount(null))
                    .onlineUsers(0L) // TODO: 实现在线用户统计
                    .systemStartTime(LocalDateTime.now().minusDays(30)) // 示例数据
                    .version("1.0.0")
                    .build();

            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("查询系统统计信息失败: error={}", e.getMessage());
            throw e;
        }
    }

    /**
     * 系统统计信息DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class SystemStats {
        private Long totalUsers;
        private Long onlineUsers;
        private LocalDateTime systemStartTime;
        private String version;
    }
}