package com.example.shorturl.common.aspect;

import com.example.shorturl.common.annotation.RequiresLog;
import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.security.JwtUtils;
import com.example.shorturl.model.entity.UserOperationLog;
import com.example.shorturl.service.AsyncLogService;
import com.example.shorturl.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 操作日志AOP切面
 * <p>
 * 模块职责：
 * - 自动记录用户操作日志
 * - 通过注解标记需要记录的方法
 * - 异步保存日志到数据库
 * - 支持敏感信息过滤
 * <p>
 * 核心功能：
 * - 方法执行前后记录
 * - 异常情况处理
 * - 请求参数和响应结果记录
 * - 用户信息和IP地址获取
 * <p>
 * 依赖关系：
 * - 被@RequiresLog注解的方法自动触发
 * - 依赖AsyncLogService异步保存日志
 * - 与JwtUtils配合获取用户信息
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final AsyncLogService asyncLogService;

    private final UserService userService;

    private final JwtUtils jwtUtils;

    /**
     * 定义切点：匹配所有被@RequiresLog注解的方法
     */
    @Pointcut("@annotation(com.example.shorturl.common.annotation.RequiresLog)")
    public void logPointcut() {
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解信息
        RequiresLog requiresLog = getAnnotation(joinPoint);
        if (requiresLog == null) {
            return joinPoint.proceed();
        }

        // 创建操作日志对象
        UserOperationLog operationLog = new UserOperationLog();
        long startTime = System.currentTimeMillis();

        try {
            // 获取HttpServletRequest
            HttpServletRequest request = getHttpServletRequest();
            if (request == null) {
                log.warn("无法获取HttpServletRequest");
                return joinPoint.proceed();
            }

            // 记录请求信息
            fillRequestInfo(operationLog, request, requiresLog);

            // 记录请求参数
            if (requiresLog.logParams()) {
                String requestParams = filterSensitiveParams(joinPoint.getArgs());
                operationLog.setRequestParams(requestParams);
            }

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 记录成功信息
            operationLog.setStatus(1); // 成功
            operationLog.setOperationTime(LocalDateTime.now());

            // 记录响应结果
            if (requiresLog.logResult() && result != null) {
                String responseResult = convertToJson(result);
                operationLog.setResponseResult(responseResult);
            }

            return result;

        } catch (Exception e) {
            // 记录失败信息
            operationLog.setStatus(0); // 失败
            operationLog.setErrorMessage(getErrorMessage(e));
            operationLog.setOperationTime(LocalDateTime.now());

            // 重新抛出异常
            throw e;
        } finally {
            // 异步保存操作日志
            try {
                asyncLogService.saveOperationLog(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }

    /**
     * 获取@RequiresLog注解
     */
    private RequiresLog getAnnotation(ProceedingJoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Class<?> targetClass = joinPoint.getTarget().getClass();

            // 获取方法上的注解
            java.lang.reflect.Method method = Arrays.stream(targetClass.getMethods())
                    .filter(m -> m.getName().equals(methodName))
                    .filter(m -> m.isAnnotationPresent(RequiresLog.class))
                    .findFirst()
                    .orElse(null);

            return Optional.ofNullable(method)
                    .map(m -> m.getAnnotation(RequiresLog.class))
                    .orElse(null);
        } catch (Exception e) {
            log.error("获取注解失败", e);
            return null;
        }
    }

    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return Optional.ofNullable(attributes)
                    .map(ServletRequestAttributes::getRequest)
                    .orElse(null);
        } catch (Exception e) {
            log.error("获取HttpServletRequest失败", e);
            return null;
        }
    }

    /**
     * 填充请求信息
     */
    private void fillRequestInfo(UserOperationLog operationLog, HttpServletRequest request, RequiresLog requiresLog) {
        // 获取用户信息
        Long userId = getCurrentUserId(request);
        operationLog.setUserId(userId);

        // 获取操作类型
        String operationType = requiresLog.type();
        if (operationType == null || operationType.trim().isEmpty()) {
            operationType = extractOperationType(request.getMethod());
        }
        operationLog.setOperationType(operationType);

        // 获取操作模块
        String module = requiresLog.module();
        if (module == null || module.trim().isEmpty()) {
            module = extractModule(request.getRequestURI());
        }
        operationLog.setModule(module);

        // 获取操作描述
        String operationDesc = requiresLog.description();
        if (operationDesc == null || operationDesc.trim().isEmpty()) {
            operationDesc = operationType + "操作";
        }
        operationLog.setOperationDesc(operationDesc);

        // 获取客户端IP
        String ipAddress = getClientIp(request);
        operationLog.setIpAddress(ipAddress);

        // 获取User-Agent
        String userAgent = request.getHeader("User-Agent");
        operationLog.setUserAgent(userAgent);

        // 获取请求路径
        operationLog.setRequestPath(request.getRequestURI());

        // 获取请求方法
        operationLog.setRequestMethod(request.getMethod());
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            // 从Authorization头获取Token
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtils.validateToken(token)) {
                    return jwtUtils.extractUserId(token);
                }
            }

            // 从SecurityContext获取
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String username && !"anonymousUser".equals(username)) {
                    return Optional.ofNullable(userService.getUserByUsername(username))
                            .map(com.example.shorturl.model.entity.User::getId)
                            .orElse(null);
                }
            }

            return null;
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return null;
        }
    }

    /**
     * 过滤敏感参数
     */
    private String filterSensitiveParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        try {
            // 过滤敏感字段
            return Arrays.stream(args)
                    .map(this::filterSensitiveFields)
                    .map(this::convertToJson)
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.error("过滤敏感参数失败", e);
            return "[参数过滤失败]";
        }
    }

    /**
     * 过滤对象中的敏感字段
     */
    private Object filterSensitiveFields(Object obj) {
        if (obj == null) {
            return null;
        }

        // 这里可以实现更复杂的敏感字段过滤逻辑
        // 目前简单返回原对象，实际应用中需要根据业务需求实现
        return obj;
    }

    /**
     * 转换为JSON字符串
     */
    private String convertToJson(Object obj) {
        if (obj == null) {
            return "";
        }

        try {
            // 简单实现，实际应用中可以使用Jackson或Gson
            return obj.toString();
        } catch (Exception e) {
            log.error("转换为JSON失败", e);
            return "[转换失败]";
        }
    }

    /**
     * 获取错误信息
     */
    private String getErrorMessage(Exception e) {
        if (e instanceof BusinessException be) {
            return String.format("业务异常: %s (错误码: %d)", be.getMessage(), be.getCode());
        }
        return Objects.requireNonNullElse(e.getMessage(), "未知错误");
    }

    /**
     * 根据HTTP方法提取操作类型
     */
    private String extractOperationType(String httpMethod) {
        return switch (httpMethod.toUpperCase()) {
            case "GET" -> "QUERY";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "OTHER";
        };
    }

    /**
     * 根据请求URI提取模块
     */
    private String extractModule(String requestUri) {
        if (requestUri == null) {
            return "UNKNOWN";
        }

        // 根据URI路径判断模块
        if (requestUri.contains("/users")) {
            return "USER_MANAGEMENT";
        } else if (requestUri.contains("/urls")) {
            return "URL_MANAGEMENT";
        } else if (requestUri.contains("/logs")) {
            return "LOG_MANAGEMENT";
        } else if (requestUri.contains("/system")) {
            return "SYSTEM_MANAGEMENT";
        } else if (requestUri.contains("/auth")) {
            return "AUTHENTICATION";
        }

        return "GENERAL";
    }

    /**
     * 获取客户端IP地址
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
                // 对于X-Forwarded-For，可能包含多个IP，取第一个非unknown的IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // 如果所有头都没有，使用remoteAddr
        return request.getRemoteAddr();
    }
}
