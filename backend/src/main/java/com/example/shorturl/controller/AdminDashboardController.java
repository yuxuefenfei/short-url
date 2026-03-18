package com.example.shorturl.controller;

import com.example.shorturl.common.annotation.RequiresLog;
import com.example.shorturl.common.response.ApiResponse;
import com.example.shorturl.dao.AccessLogDao;
import com.example.shorturl.dao.UrlMappingDao;
import com.example.shorturl.model.entity.ShortUrlMapping;
import com.example.shorturl.model.entity.UrlAccessLog;
import com.example.shorturl.model.entity.table.ShortUrlMappingTableDef;
import com.example.shorturl.model.entity.table.UrlAccessLogTableDef;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private UrlMappingDao urlMappingDao;

    @Autowired
    private AccessLogDao accessLogDao;

    @Value("${short-url.domain:https://short.ly}")
    private String shortUrlDomain;

    @RequiresLog(type = "QUERY", module = "SYSTEM_MONITOR", description = "查询管理后台概览")
    @GetMapping("/overview")
    public ApiResponse<DashboardOverview> getOverview(@RequestParam(defaultValue = "7") Integer days) {
        DashboardOverview overview = new DashboardOverview();
        overview.setTrend(buildTrend(days));
        overview.setHotUrls(buildHotUrls());
        overview.setRecentAccess(buildRecentAccess());
        overview.setSystemMetrics(buildSystemMetrics());
        return ApiResponse.success(overview);
    }

    private TrendData buildTrend(Integer days) {
        int period = days == null || days < 1 ? 7 : days;
        LocalDate start = LocalDate.now().minusDays(period - 1L);

        Map<LocalDate, Long> clickMap = new LinkedHashMap<>();
        Map<LocalDate, Long> newUrlMap = new LinkedHashMap<>();
        for (int i = 0; i < period; i++) {
            LocalDate date = start.plusDays(i);
            clickMap.put(date, 0L);
            newUrlMap.put(date, 0L);
        }

        List<UrlAccessLog> accessLogs = accessLogDao.selectListByQuery(
                QueryWrapper.create().where(UrlAccessLogTableDef.URL_ACCESS_LOG.ACCESS_TIME.ge(start.atStartOfDay()))
        );
        for (UrlAccessLog log : accessLogs) {
            LocalDate date = log.getAccessTime().toLocalDate();
            if (clickMap.containsKey(date)) {
                clickMap.put(date, clickMap.get(date) + 1);
            }
        }

        List<ShortUrlMapping> mappings = urlMappingDao.selectListByQuery(
                QueryWrapper.create().where(ShortUrlMappingTableDef.SHORT_URL_MAPPING.CREATED_TIME.ge(start.atStartOfDay()))
        );
        for (ShortUrlMapping mapping : mappings) {
            LocalDate date = mapping.getCreatedTime().toLocalDate();
            if (newUrlMap.containsKey(date)) {
                newUrlMap.put(date, newUrlMap.get(date) + 1);
            }
        }

        TrendData trendData = new TrendData();
        trendData.setDates(clickMap.keySet().stream().map(date -> date.format(DateTimeFormatter.ISO_DATE)).toList());
        trendData.setClicks(new ArrayList<>(clickMap.values()));
        trendData.setNewUrls(new ArrayList<>(newUrlMap.values()));
        return trendData;
    }

    private List<HotUrlItem> buildHotUrls() {
        List<ShortUrlMapping> mappings = urlMappingDao.selectListByQuery(
                QueryWrapper.create().orderBy(ShortUrlMappingTableDef.SHORT_URL_MAPPING.CLICK_COUNT, false)
        );

        return mappings.stream()
                .limit(5)
                .map(mapping -> {
                    HotUrlItem item = new HotUrlItem();
                    item.setShortKey(mapping.getShortKey());
                    item.setShortUrl(shortUrlDomain + "/" + mapping.getShortKey());
                    item.setTitle(mapping.getTitle());
                    item.setClickCount(mapping.getClickCount() == null ? 0L : mapping.getClickCount());
                    return item;
                })
                .toList();
    }

    private List<RecentAccessItem> buildRecentAccess() {
        List<UrlAccessLog> logs = accessLogDao.selectListByQuery(
                QueryWrapper.create().orderBy(UrlAccessLogTableDef.URL_ACCESS_LOG.ACCESS_TIME, false)
        );

        return logs.stream()
                .limit(8)
                .map(log -> {
                    RecentAccessItem item = new RecentAccessItem();
                    item.setShortKey(log.getShortKey());
                    item.setIpAddress(log.getIpAddress());
                    item.setAccessTime(log.getAccessTime());
                    return item;
                })
                .toList();
    }

    private SystemMetrics buildSystemMetrics() {
        SystemMetrics metrics = new SystemMetrics();

        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        metrics.setMemoryUsage(maxMemory <= 0 ? 0 : (int) Math.min(100, usedMemory * 100 / maxMemory));
        metrics.setCpuUsage(readCpuUsage());
        metrics.setDiskUsage(readDiskUsage());
        metrics.setNetworkLatency(10);
        return metrics;
    }

    private int readCpuUsage() {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                double load = osBean.getCpuLoad();
                if (load >= 0) {
                    return (int) Math.round(load * 100);
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private int readDiskUsage() {
        try {
            FileStore store = FileSystems.getDefault().getFileStores().iterator().next();
            long total = store.getTotalSpace();
            long used = total - store.getUsableSpace();
            return total <= 0 ? 0 : (int) Math.min(100, used * 100 / total);
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Data
    public static class DashboardOverview {
        private TrendData trend;
        private List<HotUrlItem> hotUrls;
        private List<RecentAccessItem> recentAccess;
        private SystemMetrics systemMetrics;
    }

    @Data
    public static class TrendData {
        private List<String> dates;
        private List<Long> clicks;
        private List<Long> newUrls;
    }

    @Data
    public static class HotUrlItem {
        private String shortKey;
        private String shortUrl;
        private String title;
        private Long clickCount;
    }

    @Data
    public static class RecentAccessItem {
        private String shortKey;
        private String ipAddress;
        private LocalDateTime accessTime;
    }

    @Data
    public static class SystemMetrics {
        private Integer cpuUsage;
        private Integer memoryUsage;
        private Integer diskUsage;
        private Integer networkLatency;
    }
}
