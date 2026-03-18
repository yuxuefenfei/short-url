package com.example.shorturl.controller;

import com.example.shorturl.common.annotation.RequiresLog;
import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.common.response.PageResult;
import com.example.shorturl.model.entity.UserOperationLog;
import com.example.shorturl.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/operation-logs")
public class AdminOperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @RequiresLog(type = "QUERY", module = "SYSTEM_MONITOR", description = "查询操作日志")
    @GetMapping
    public ApiResponse<PageResult<UserOperationLog>> getOperationLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return ApiResponse.success(
                operationLogService.getOperationLogs(page, size, keyword, module, operationType, status, startDate, endDate)
        );
    }

    @RequiresLog(type = "QUERY", module = "SYSTEM_MONITOR", description = "查询操作日志统计")
    @GetMapping("/stats")
    public ApiResponse<OperationLogService.OperationLogStats> getOperationLogStats() {
        return ApiResponse.success(operationLogService.getOperationStats());
    }
}
