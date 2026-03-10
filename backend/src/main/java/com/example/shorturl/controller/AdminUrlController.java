package com.example.shorturl.controller;

import com.example.shorturl.common.annotation.RequiresLog;
import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.common.response.PageResult;
import com.example.shorturl.model.entity.ShortUrlMapping;
import com.example.shorturl.model.entity.table.ShortUrlMappingTableDef;
import com.example.shorturl.service.UrlService;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员URL控制器
 * <p>
 * 模块职责：
 * - 提供短网址管理功能
 * - 处理URL的增删改查操作
 * - 提供批量操作和统计功能
 * <p>
 * 安全特性：
 * - 需要ADMIN角色权限
 * - 所有操作记录审计日志
 * - 输入验证和异常处理
 * <p>
 * 依赖关系：
 * - 被前端管理后台调用
 * - 依赖UrlService处理业务逻辑
 * - 与缓存系统配合实现高性能
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminUrlController {

    @Autowired
    private UrlService urlService;

    /**
     * 获取短网址列表
     *
     * @param page    页码
     * @param size    每页数量
     * @param keyword 搜索关键词
     * @param status  状态筛选
     * @return 短网址列表分页结果
     */
    @RequiresLog(type = "QUERY", module = "URL_MANAGEMENT", description = "查询短网址列表")
    @GetMapping("/urls")
    public ApiResponse<PageResult<ShortUrlMapping>> getUrlList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        log.info("查询短网址列表: page={}, size={}, keyword={}, status={}", page, size, keyword, status);

        try {
            // 构建查询条件
            QueryWrapper queryWrapper =
                    QueryWrapper.create();

            if (keyword != null && !keyword.trim().isEmpty()) {
                queryWrapper.where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.ORIGINAL_URL.like(keyword))
                        .or(ShortUrlMappingTableDef.SHORT_URL_MAPPING.TITLE.like(keyword))
                        .or(ShortUrlMappingTableDef.SHORT_URL_MAPPING.SHORT_KEY.like(keyword));
            }

            if (status != null) {
                queryWrapper.eq(ShortUrlMapping::getStatus, status);
            }

            // 按创建时间倒序
            queryWrapper.orderBy(ShortUrlMapping::getCreatedTime, false);

            // 执行查询
            List<ShortUrlMapping> urlList = urlService.getUrlList(page, size, keyword, status);
            Long total = urlService.getUrlCount(keyword, status);

            // 构建分页结果
            PageResult<ShortUrlMapping> result = new PageResult<>(urlList, total, page, size);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询短网址列表失败: error={}", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取短网址详情
     *
     * @param id 短网址ID
     * @return 短网址详细信息
     */
    @RequiresLog(type = "QUERY", module = "URL_MANAGEMENT", description = "查询短网址详情")
    @GetMapping("/urls/{id}")
    public ApiResponse<ShortUrlMapping> getUrlDetail(@PathVariable Long id) {
        log.info("查询短网址详情: id={}", id);

        try {
            // 添加Service层方法来获取URL详情
            ShortUrlMapping mapping = urlService.getUrlById(id);
            if (mapping == null) {
                return ApiResponse.error(404, "短网址不存在");
            }
            return ApiResponse.success(mapping);
        } catch (Exception e) {
            log.error("查询短网址详情失败: id={}, error={}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 更新短网址状态
     *
     * @param id     短网址ID
     * @param status 状态 (1:正常, 0:禁用)
     * @return 操作结果
     */
    @RequiresLog(type = "UPDATE", module = "URL_MANAGEMENT", description = "更新短网址状态")
    @PutMapping("/urls/{id}/status")
    public ApiResponse<Void> updateUrlStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {

        log.info("更新短网址状态: id={}, status={}", id, status);

        try {
            ShortUrlMapping mapping = urlService.getUrlById(id);
            if (mapping == null) {
                return ApiResponse.error(404, "短网址不存在");
            }

            urlService.updateUrlStatus(id, status);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("更新短网址状态失败: id={}, status={}, error={}", id, status, e.getMessage());
            throw e;
        }
    }

    /**
     * 删除短网址
     *
     * @param id 短网址ID
     * @return 操作结果
     */
    @RequiresLog(type = "DELETE", module = "URL_MANAGEMENT", description = "删除短网址")
    @DeleteMapping("/urls/{id}")
    public ApiResponse<Void> deleteUrl(@PathVariable Long id) {
        log.info("删除短网址: id={}", id);

        try {
            ShortUrlMapping mapping = urlService.getUrlById(id);
            if (mapping == null) {
                return ApiResponse.error(404, "短网址不存在");
            }

            // 执行删除
            urlService.deleteUrl(id);

            return ApiResponse.success();
        } catch (Exception e) {
            log.error("删除短网址失败: id={}, error={}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 批量更新短网址状态
     *
     * @param ids    短网址ID列表
     * @param status 状态 (1:正常, 0:禁用)
     * @return 操作结果
     */
    @RequiresLog(type = "BATCH_UPDATE", module = "URL_MANAGEMENT", description = "批量更新短网址状态")
    @PutMapping("/urls/batch-status")
    public ApiResponse<Void> batchUpdateStatus(
            @RequestParam List<Long> ids,
            @RequestParam Integer status) {

        log.info("批量更新短网址状态: ids={}, status={}", ids, status);

        try {
            if (ids == null || ids.isEmpty()) {
                return ApiResponse.error(400, "ID列表不能为空");
            }

            // 批量更新
            for (Long id : ids) {
                urlService.updateUrlStatus(id, status);
            }

            return ApiResponse.success();
        } catch (Exception e) {
            log.error("批量更新短网址状态失败: ids={}, status={}, error={}", ids, status, e.getMessage());
            throw e;
        }
    }

    /**
     * 批量删除短网址
     *
     * @param ids 短网址ID列表
     * @return 操作结果
     */
    @RequiresLog(type = "BATCH_DELETE", module = "URL_MANAGEMENT", description = "批量删除短网址")
    @DeleteMapping("/urls/batch")
    public ApiResponse<Void> batchDeleteUrls(@RequestBody List<Long> ids) {
        log.info("批量删除短网址: ids={}", ids);

        try {
            if (ids == null || ids.isEmpty()) {
                return ApiResponse.error(400, "ID列表不能为空");
            }

            // 批量删除
            for (Long id : ids) {
                urlService.deleteUrl(id);
            }

            return ApiResponse.success();
        } catch (Exception e) {
            log.error("批量删除短网址失败: ids={}, error={}", ids, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取短网址统计数据
     *
     * @param shortKey 短网址Key
     * @return 统计信息
     */
    @RequiresLog(type = "QUERY", module = "URL_STATISTICS", description = "查询短网址统计")
    @GetMapping("/urls/{shortKey}/stats")
    public ApiResponse<AdminUrlStats> getUrlStats(@PathVariable String shortKey) {
        log.info("查询短网址统计: shortKey={}", shortKey);

        try {
            com.example.shorturl.service.UrlService.UrlStats stats = urlService.getUrlStats(shortKey);

            // Convert to controller's AdminUrlStats DTO
            AdminUrlStats dto = new AdminUrlStats();
            dto.setShortKey(stats.getShortKey());
            dto.setOriginalUrl(stats.getOriginalUrl());
            dto.setTotalClicks(stats.getTotalClicks());
            dto.setTodayClicks(stats.getTodayClicks());
            dto.setCreatedTime(stats.getCreatedTime());
            dto.setStatus(stats.getStatus());

            return ApiResponse.success(dto);
        } catch (BusinessException e) {
            return ApiResponse.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询短网址统计失败: shortKey={}, error={}", shortKey, e.getMessage());
            throw e;
        }
    }

    /**
     * 短网址统计信息DTO
     */
    @lombok.Data
    public static class AdminUrlStats {
        private String shortKey;
        private String originalUrl;
        private Long totalClicks;
        private Long todayClicks;
        private LocalDateTime createdTime;
        private LocalDateTime lastAccessTime;
        private Integer status;

        // Default constructor
        public AdminUrlStats() {
        }
    }
}