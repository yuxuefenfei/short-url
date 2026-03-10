package com.example.shorturl.common.exception;

import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.common.response.ResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 模块职责：
 * - 统一处理所有控制器抛出的异常
 * - 转换为标准的ApiResponse格式
 * - 记录异常日志
 * - 区分不同类型的异常处理策略
 * <p>
 * 异常处理策略：
 * - 业务异常：返回具体的业务错误码
 * - 参数验证异常：返回详细的字段错误信息
 * - 认证授权异常：返回权限相关错误信息
 * - 系统异常：记录详细日志，返回通用错误信息
 * <p>
 * 依赖关系：
 * - 使用@RestControllerAdvice注解自动生效
 * - 与BusinessException配合处理业务异常
 * - 与ApiResponse配合统一响应格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Business exception: code={}, message={}, path={}",
                e.getCode(), e.getMessage(), request.getRequestURI());

        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数验证异常 (MethodArgumentNotValidException)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation exception: {}, path={}", errorMsg, request.getRequestURI());

        return ApiResponse.error(ResponseStatus.BAD_REQUEST.getCode(),
                "参数验证失败: " + errorMsg);
    }

    /**
     * 处理参数绑定异常 (BindException)
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<?> handleBindException(BindException e, HttpServletRequest request) {
        String errorMsg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Bind exception: {}, path={}", errorMsg, request.getRequestURI());

        return ApiResponse.error(ResponseStatus.BAD_REQUEST.getCode(),
                "参数绑定失败: " + errorMsg);
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler({
            AuthenticationException.class,
            BadCredentialsException.class,
            AuthenticationCredentialsNotFoundException.class
    })
    public ApiResponse<?> handleAuthException(Exception e, HttpServletRequest request) {
        log.warn("Authentication exception: {}, path={}", e.getMessage(), request.getRequestURI());

        return ApiResponse.error(ResponseStatus.UNAUTHORIZED.getCode(),
                "认证失败: " + e.getMessage());
    }

    /**
     * 处理授权异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("Access denied: {}, path={}", e.getMessage(), request.getRequestURI());

        return ApiResponse.error(ResponseStatus.FORBIDDEN.getCode(),
                "权限不足，无法访问该资源");
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResponse<?> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("No handler found: {} {}, path={}",
                request.getMethod(), request.getRequestURI(), request.getRequestURI());

        return ApiResponse.error(ResponseStatus.NOT_FOUND.getCode(),
                "请求路径不存在");
    }

    /**
     * 处理参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<?> handleMissingParameterException(MissingServletRequestParameterException e,
                                                          HttpServletRequest request) {
        log.warn("Missing parameter: {}, path={}", e.getParameterName(), request.getRequestURI());

        return ApiResponse.error(ResponseStatus.BAD_REQUEST.getCode(),
                "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理参数类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                      HttpServletRequest request) {
        log.warn("Type mismatch: {} should be {}, path={}",
                e.getName(), e.getRequiredType(), request.getRequestURI());

        return ApiResponse.error(ResponseStatus.BAD_REQUEST.getCode(),
                "参数类型错误: " + e.getName() + " 应该是 " +
                        (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "正确类型"));
    }

    /**
     * 处理JSON解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
                                                                HttpServletRequest request) {
        log.warn("JSON parse error: {}, path={}", e.getMessage(), request.getRequestURI());

        return ApiResponse.error(ResponseStatus.BAD_REQUEST.getCode(),
                "请求数据格式错误，请检查JSON格式");
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ApiResponse<?> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("Null pointer exception: path={}", request.getRequestURI(), e);

        return ApiResponse.error(ResponseStatus.INTERNAL_ERROR.getCode(),
                "服务器内部错误，请稍后重试");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("Runtime exception: path={}", request.getRequestURI(), e);

        return ApiResponse.error(ResponseStatus.INTERNAL_ERROR.getCode(),
                "服务器内部错误，请稍后重试");
    }

    /**
     * 处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleSystemException(Exception e, HttpServletRequest request) {
        log.error("System exception: path={}", request.getRequestURI(), e);

        // 记录完整的异常堆栈信息用于问题排查
        log.error("Full exception stack trace:", e);

        return ApiResponse.error(ResponseStatus.INTERNAL_ERROR.getCode(),
                "服务器内部错误，请稍后重试");
    }
}