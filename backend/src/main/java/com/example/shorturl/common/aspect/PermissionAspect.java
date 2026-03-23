package com.example.shorturl.common.aspect;

import com.example.shorturl.common.annotation.RequiresPermission;
import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.common.security.JwtUtils;
import com.example.shorturl.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
@RequiredArgsConstructor
public class PermissionAspect {
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserService userService;

    private final JwtUtils jwtUtils;

    @Around("@annotation(com.example.shorturl.common.annotation.RequiresPermission) || " +
            "@within(com.example.shorturl.common.annotation.RequiresPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Optional.ofNullable(attributes)
                .map(ServletRequestAttributes::getRequest)
                .orElse(null);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = getAnnotation(method);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                throw new BusinessException(ResponseStatus.UNAUTHORIZED);
            }

            validatePermission(userId, annotation);

            if (annotation.logAccess()) {
                logAccess(request, userId, method, true, null);
            }

            return joinPoint.proceed();
        } catch (BusinessException e) {
            if (annotation.logAccess()) {
                logAccess(request, getCurrentUserId(request), method, false, e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            if (annotation.logAccess()) {
                logAccess(request, getCurrentUserId(request), method, false, "系统错误: " + e.getMessage());
            }
            log.error("权限校验异常", e);
            throw new BusinessException(ResponseStatus.INTERNAL_ERROR);
        }
    }

    private RequiresPermission getAnnotation(Method method) {
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        return annotation != null
                ? annotation
                : method.getDeclaringClass().getAnnotation(RequiresPermission.class);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User user) {
                return Optional.ofNullable(userService.getUserByUsername(user.getUsername()))
                        .map(com.example.shorturl.model.entity.User::getId)
                        .orElse(null);
            }

            String token = extractToken(request);
            return token == null ? null : jwtUtils.extractUserId(token);
        } catch (Exception e) {
            log.debug("获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return Optional.ofNullable(bearerToken)
                .filter(token -> token.startsWith("Bearer "))
                .map(token -> token.substring(7))
                .orElse(null);
    }

    private void validatePermission(Long userId, RequiresPermission annotation) {
        var user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResponseStatus.USER_DISABLED);
        }

        String userRole = user.getRole();
        if (ADMIN_ROLE.equals(userRole)) {
            return;
        }

        if (annotation.roles().length > 0 && checkRoles(userRole, annotation.roles(), annotation.logical())) {
            return;
        }

        if (annotation.permissions().length > 0
                && checkPermissions(userId, annotation.permissions(), annotation.logical())) {
            return;
        }

        log.warn("权限校验失败: userId={}, role={}, requiredRoles={}, requiredPermissions={}",
                userId, userRole, Arrays.toString(annotation.roles()), Arrays.toString(annotation.permissions()));
        throw new BusinessException(ResponseStatus.FORBIDDEN.getCode(), annotation.message());
    }

    private void logAccess(HttpServletRequest request, Long userId, Method method, boolean success, String errorMessage) {
        try {
            if (request == null || userId == null) {
                return;
            }

            log.info("权限访问日志: userId={}, ip={}, method={}, success={}, error={}",
                    userId,
                    request.getRemoteAddr(),
                    method.getName(),
                    success,
                    Objects.requireNonNullElse(errorMessage, "无"));
        } catch (Exception e) {
            log.error("记录访问日志失败", e);
        }
    }

    private boolean checkRoles(String userRole, String[] requiredRoles, RequiresPermission.Logical logical) {
        if (requiredRoles.length == 0) {
            return true;
        }

        List<String> requiredRolesList = List.of(requiredRoles);
        boolean match = requiredRolesList.contains(userRole);
        return switch (logical) {
            case ANY, ALL -> match;
        };
    }

    private boolean checkPermissions(Long userId, String[] requiredPermissions, RequiresPermission.Logical logical) {
        log.debug("检查具体权限: userId={}, permissions={}, logical={}",
                userId, Arrays.toString(requiredPermissions), logical);
        return true;
    }
}
