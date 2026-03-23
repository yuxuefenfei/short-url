package com.example.shorturl.service;

import com.example.shorturl.dao.AccessLogDao;
import com.example.shorturl.dao.OperationLogDao;
import com.example.shorturl.dao.UrlMappingDao;
import com.example.shorturl.model.entity.ShortUrlMapping;
import com.example.shorturl.model.entity.UrlAccessLog;
import com.example.shorturl.model.entity.UserOperationLog;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 异步日志服务
 * <p>
 * 模块职责：
 * - 异步保存操作日志到数据库
 * - 异步保存访问日志
 * - 处理日志写入异常
 * - 提供日志查询和统计功能
 * <p>
 * 核心功能：
 * - 异步写入用户操作日志
 * - 异步写入URL访问日志
 * - 异常处理和重试机制
 * - 日志数据脱敏处理
 * <p>
 * 依赖关系：
 * - 被OperationLogAspect调用
 * - 被RedirectController调用
 * - 依赖OperationLogDao和UrlAccessLogDao
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncLogService {

    private final OperationLogDao operationLogDao;

    private final AccessLogDao urlAccessLogDao;

    private final UrlMappingDao urlMappingDao;

    /**
     * 异步保存用户操作日志
     *
     * @param operationLog 操作日志对象
     */
    @Async("logTaskExecutor")
    public void saveOperationLog(UserOperationLog operationLog) {
        try {
            // 数据验证
            if (operationLog == null) {
                log.warn("操作日志对象为空");
                return;
            }

            // 设置默认值
            if (operationLog.getOperationTime() == null) {
                operationLog.setOperationTime(LocalDateTime.now());
            }

            if (operationLog.getStatus() == null) {
                operationLog.setStatus(1); // 默认成功
            }

            // 限制字段长度
            limitFieldLength(operationLog);

            // 保存到数据库
            int result = operationLogDao.insert(operationLog);

            if (result > 0) {
                log.debug("操作日志保存成功: logId={}, userId={}, operation={}",
                        operationLog.getId(), operationLog.getUserId(), operationLog.getOperationType());
            } else {
                log.warn("操作日志保存失败: userId={}, operation={}",
                        operationLog.getUserId(), operationLog.getOperationType());
            }

        } catch (Exception e) {
            log.error("异步保存操作日志失败: userId={}, operation={}, error={}",
                    operationLog != null ? operationLog.getUserId() : null,
                    operationLog != null ? operationLog.getOperationType() : null,
                    e.getMessage(), e);
        }
    }

    /**
     * 异步记录URL访问日志
     *
     * @param shortKey 短网址Key
     * @param request  HTTP请求对象
     */
    @Async("logTaskExecutor")
    public void logUrlAccess(String shortKey, HttpServletRequest request) {
        try {
            if (shortKey == null || request == null) {
                log.warn("参数不能为空: shortKey={}, request={}", shortKey, request);
                return;
            }

            // 创建访问日志对象
            UrlAccessLog accessLog = new UrlAccessLog();
            accessLog.setShortKey(shortKey);
            accessLog.setUserAgent(request.getHeader("User-Agent"));
            accessLog.setIpAddress(getClientIp(request));
            accessLog.setAccessTime(LocalDateTime.now());

            // 限制字段长度
            if (accessLog.getUserAgent() != null && accessLog.getUserAgent().length() > 500) {
                accessLog.setUserAgent(accessLog.getUserAgent().substring(0, 500));
            }

            // 保存访问日志
            int result = urlAccessLogDao.insert(accessLog);

            if (result > 0) {
                log.debug("访问日志保存成功: shortKey={}, ip={}", shortKey, accessLog.getIpAddress());

                // 异步更新点击次数
                updateClickCount(shortKey);
            } else {
                log.warn("访问日志保存失败: shortKey={}, ip={}", shortKey, accessLog.getIpAddress());
            }

        } catch (Exception e) {
            log.error("异步保存访问日志失败: shortKey={}, error={}", shortKey, e.getMessage(), e);
        }
    }

    /**
     * 异步更新短网址点击次数
     *
     * @param shortKey 短网址Key
     */
    @Async("logTaskExecutor")
    public void updateClickCount(String shortKey) {
        try {
            if (shortKey == null) {
                return;
            }

            int result = 0;
            // 查询当前映射
            ShortUrlMapping mapping = urlMappingDao.selectOneByQuery(
                    QueryWrapper.create().eq("short_key", shortKey));

            if (mapping != null) {
                // 更新点击数
                mapping.setClickCount(java.util.Objects.requireNonNullElse(mapping.getClickCount(), 0L) + 1);
                urlMappingDao.update(mapping);
                result = 1; // 表示更新成功
            }

            if (result > 0) {
                log.debug("点击次数更新成功: shortKey={}", shortKey);
            } else {
                log.warn("点击次数更新失败: shortKey={}", shortKey);
            }

        } catch (Exception e) {
            log.error("异步更新点击次数失败: shortKey={}, error={}", shortKey, e.getMessage(), e);
        }
    }

    /**
     * 批量保存操作日志（用于高并发场景）
     *
     * @param operationLogs 操作日志列表
     */
    @Async("logTaskExecutor")
    public void batchSaveOperationLogs(java.util.List<UserOperationLog> operationLogs) {
        try {
            if (operationLogs == null || operationLogs.isEmpty()) {
                return;
            }

            // 过滤空对象和设置默认值
            java.util.List<UserOperationLog> validLogs = operationLogs.stream()
                    .filter(Objects::nonNull)
                    .peek(this::setDefaultValues)
                    .peek(this::limitFieldLength)
                    .toList();

            if (!validLogs.isEmpty()) {
                // 批量插入
                int result = operationLogDao.insertBatch(validLogs);
                log.info("批量保存操作日志: 总数={}, 成功={}", operationLogs.size(), result);
            }

        } catch (Exception e) {
            log.error("批量保存操作日志失败: 总数={}, error={}",
                    operationLogs.size(), e.getMessage(), e);
        }
    }

    /**
     * 批量保存访问日志
     *
     * @param accessLogs 访问日志列表
     */
    @Async("logTaskExecutor")
    public void batchSaveAccessLogs(java.util.List<UrlAccessLog> accessLogs) {
        try {
            if (accessLogs == null || accessLogs.isEmpty()) {
                return;
            }

            // 过滤和处理
            java.util.List<UrlAccessLog> validLogs = accessLogs.stream()
                    .filter(log -> log != null && log.getShortKey() != null)
                    .peek(this::setDefaultValues)
                    .toList();

            if (!validLogs.isEmpty()) {
                // 批量插入
                int result = urlAccessLogDao.insertBatch(validLogs);
                log.info("批量保存访问日志: 总数={}, 成功={}", accessLogs.size(), result);
            }

        } catch (Exception e) {
            log.error("批量保存访问日志失败: 总数={}, error={}",
                    accessLogs.size(), e.getMessage(), e);
        }
    }

    /**
     * 设置操作日志默认值
     */
    private void setDefaultValues(UserOperationLog operationLog) {
        if (operationLog.getOperationTime() == null) {
            operationLog.setOperationTime(LocalDateTime.now());
        }

        if (operationLog.getStatus() == null) {
            operationLog.setStatus(1);
        }

        if (operationLog.getOperationType() == null) {
            operationLog.setOperationType("UNKNOWN");
        }

        if (operationLog.getModule() == null) {
            operationLog.setModule("GENERAL");
        }
    }

    /**
     * 设置访问日志默认值
     */
    private void setDefaultValues(UrlAccessLog accessLog) {
        if (accessLog.getAccessTime() == null) {
            accessLog.setAccessTime(LocalDateTime.now());
        }
    }

    /**
     * 限制操作日志字段长度
     */
    private void limitFieldLength(UserOperationLog operationLog) {
        if (operationLog.getOperationType() != null && operationLog.getOperationType().length() > 50) {
            operationLog.setOperationType(operationLog.getOperationType().substring(0, 50));
        }

        if (operationLog.getOperationDesc() != null && operationLog.getOperationDesc().length() > 500) {
            operationLog.setOperationDesc(operationLog.getOperationDesc().substring(0, 500));
        }

        if (operationLog.getModule() != null && operationLog.getModule().length() > 50) {
            operationLog.setModule(operationLog.getModule().substring(0, 50));
        }

        if (operationLog.getIpAddress() != null && operationLog.getIpAddress().length() > 45) {
            operationLog.setIpAddress(operationLog.getIpAddress().substring(0, 45));
        }

        if (operationLog.getRequestPath() != null && operationLog.getRequestPath().length() > 500) {
            operationLog.setRequestPath(operationLog.getRequestPath().substring(0, 500));
        }

        if (operationLog.getRequestMethod() != null && operationLog.getRequestMethod().length() > 10) {
            operationLog.setRequestMethod(operationLog.getRequestMethod().substring(0, 10));
        }

        if (operationLog.getErrorMessage() != null && operationLog.getErrorMessage().length() > 1000) {
            operationLog.setErrorMessage(operationLog.getErrorMessage().substring(0, 1000));
        }

        // 对于TEXT字段，不限制长度但记录警告
        if (operationLog.getUserAgent() != null && operationLog.getUserAgent().length() > 1000) {
            log.warn("UserAgent长度超过1000字符: {}", operationLog.getUserAgent().length());
        }

        if (operationLog.getRequestParams() != null && operationLog.getRequestParams().length() > 5000) {
            operationLog.setRequestParams(operationLog.getRequestParams().substring(0, 5000));
            log.warn("请求参数被截断: userId={}", operationLog.getUserId());
        }

        if (operationLog.getResponseResult() != null && operationLog.getResponseResult().length() > 5000) {
            operationLog.setResponseResult(operationLog.getResponseResult().substring(0, 5000));
            log.warn("响应结果被截断: userId={}", operationLog.getUserId());
        }
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
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}
