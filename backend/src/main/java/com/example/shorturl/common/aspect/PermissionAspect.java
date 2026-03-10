package com.example.shorturl.common.aspect;

import com.example.shorturl.common.annotation.RequiresPermission;
import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.common.security.JwtUtils;
import com.example.shorturl.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 权限控制切面
 * <p>
 * 模块职责：
 * - 拦截带有@RequiresPermission注解的方法
 * - 验证用户权限
 * - 记录访问日志
 * - 处理权限验证失败
 * <p>
 * AOP特性：
 * - 环绕通知处理权限验证
 * - 支持方法级别和类级别注解
 * - 与Spring Security集成
 * <p>
 * 依赖关系：
 * - 拦截@RequiresPermission注解
 * - 依赖UserService获取用户信息
 * - 依赖JwtUtils解析Token
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 权限验证环绕通知
     */
    @Around("@annotation(com.example.shorturl.common.annotation.RequiresPermission) || " +
            "@within(com.example.shorturl.common.annotation.RequiresPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = getAnnotation(method);

        if (annotation == null) {
            // 如果没有注解，直接执行方法
            return joinPoint.proceed();
        }

        try {
            // 获取当前用户信息
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                throw new BusinessException(ResponseStatus.UNAUTHORIZED);
            }

            // 验证权限
            validatePermission(userId, annotation);

            // 记录访问日志
            if (annotation.logAccess()) {
                logAccess(request, userId, method, true, null);
            }

            // 执行目标方法
            return joinPoint.proceed();

        } catch (BusinessException e) {
            // 记录失败的访问日志
            if (annotation.logAccess()) {
                Long userId = getCurrentUserId(request);
                logAccess(request, userId, method, false, e.getMessage());
            }

            // 重新抛出异常
            throw e;

        } catch (Exception e) {
            // 记录系统错误
            if (annotation.logAccess()) {
                Long userId = getCurrentUserId(request);
                logAccess(request, userId, method, false, "系统错误: " + e.getMessage());
            }

            log.error("权限验证过程中发生异常: ", e);
            throw new BusinessException(ResponseStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取方法上的@RequiresPermission注解
     */
    private RequiresPermission getAnnotation(Method method) {
        // 优先获取方法上的注解
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        if (annotation != null) {
            return annotation;
        }

        // 如果方法上没有，获取类上的注解
        return method.getDeclaringClass().getAnnotation(RequiresPermission.class);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            // 从SecurityContext获取
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User user) {
                // 这里需要从数据库获取用户ID
                return userService.getUserByUsername(user.getUsername()).getId();
            }

            // 从JWT Token获取
            String token = extractToken(request);
            if (token != null) {
                return jwtUtils.extractUserId(token);
            }

        } catch (Exception e) {
            log.debug("获取用户ID失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 验证用户权限
     */
    private void validatePermission(Long userId, RequiresPermission annotation) {
        // 获取用户信息
        var user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResponseStatus.USER_DISABLED);
        }

        String userRole = user.getRole();
        log.debug("权限验证: userId={}, role={}", userId, userRole);

        // 如果是管理员，拥有所有权限
        if ("ADMIN".equals(userRole)) {
            log.debug("管理员权限通过: userId={}", userId);
            return;
        }

        // 验证角色权限
        if (annotation.roles().length > 0) {
            boolean roleMatch = checkRoles(userRole, annotation.roles(), annotation.logical());
            if (roleMatch) {
                log.debug("角色权限通过: userId={}, role={}", userId, userRole);
                return;
            }
        }

        // 验证具体权限（这里可以实现更细粒度的权限控制）
        if (annotation.permissions().length > 0) {
            boolean permissionMatch = checkPermissions(userId, annotation.permissions(), annotation.logical());
            if (permissionMatch) {
                log.debug("具体权限通过: userId={}", userId);
                return;
            }
        }

        // 权限验证失败
        log.warn("权限验证失败: userId={}, role={}, requiredRoles={}, requiredPermissions={}",
                userId, userRole, Arrays.toString(annotation.roles()), Arrays.toString(annotation.permissions()));

        throw new BusinessException(ResponseStatus.FORBIDDEN.getCode(), annotation.message());
    }

    /**
     * 记录访问日志
     */
    private void logAccess(HttpServletRequest request, Long userId, Method method, boolean success, String errorMessage) {
        try {
            // 记录访问日志到操作日志系统
            if (request != null && userId != null) {
                // 构建操作描述
                String operationDesc = String.format("访问方法: %s.%s",
                        method.getDeclaringClass().getSimpleName(), method.getName());

                // 记录到日志系统
                log.info("权限访问日志: userId={}, ip={}, method={}, success={}, error={}",
                        userId,
                        request.getRemoteAddr(),
                        method.getName(),
                        success,
                        errorMessage != null ? errorMessage : "无");

                // 如果需要，可以调用AsyncLogService记录到数据库
                // asyncLogService.logPermissionAccess(userId, request, method, success, errorMessage);
            }
        } catch (Exception e) {
            log.error("记录访问日志失败: ", e);
        }
    }

    /**
     * 检查角色权限
     */
    private boolean checkRoles(String userRole, String[] requiredRoles, RequiresPermission.Logical logical) {
        if (requiredRoles.length == 0) {
            return true;
        }

        List<String> requiredRolesList = Arrays.asList(requiredRoles);
        boolean match = requiredRolesList.contains(userRole);

        if (logical == RequiresPermission.Logical.ANY) {
            return match;
        } else {
            // AND逻辑，需要匹配所有角色（通常不用于角色检查）
            return match;
        }
    }

    /**
     * 检查具体权限
     */
    private boolean checkPermissions(Long userId, String[] requiredPermissions, RequiresPermission.Logical logical) {
        // 这里可以实现更细粒度的权限控制
        // 例如从数据库查询用户的具体权限
        // 目前简单返回true，实际项目中需要根据业务需求实现
        log.debug("检查具体权限: userId={}, permissions={}", userId, Arrays.toString(requiredPermissions));
        return true;
    }
}