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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UrlMappingDao urlMappingDao;

    private final AccessLogDao accessLogDao;

    @Value("${short-url.domain:https://short.ly}")
    private String shortUrlDomain;

    @Value("${server.port:8080}")
    private int serverPort;

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
        int period = Optional.ofNullable(days)
                .filter(value -> value > 0)
                .orElse(7);
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
                    item.setClickCount(Objects.requireNonNullElse(mapping.getClickCount(), 0L));
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

        metrics.setMemoryUsage(readMemoryUsage());
        metrics.setCpuUsage(readCpuUsage());
        metrics.setDiskUsage(readDiskUsage());
        metrics.setNetworkLatency(readNetworkLatency());
        return metrics;
    }

    private int readCpuUsage() {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                double load = osBean.getCpuLoad();
                if (load >= 0) {
                    return clampPercent((int) Math.round(load * 100));
                }

                load = osBean.getProcessCpuLoad();
                if (load >= 0) {
                    return clampPercent((int) Math.round(load * 100));
                }
            }

            // Fallback: 使用系统负载均值估算CPU使用率
            double loadAverage = bean.getSystemLoadAverage();
            int processors = Runtime.getRuntime().availableProcessors();
            if (loadAverage >= 0 && processors > 0) {
                return clampPercent((int) Math.round((loadAverage / processors) * 100));
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private int readMemoryUsage() {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                long totalPhysical = osBean.getTotalMemorySize();
                long freePhysical = osBean.getFreeMemorySize();
                if (totalPhysical > 0 && freePhysical >= 0 && freePhysical <= totalPhysical) {
                    long usedPhysical = totalPhysical - freePhysical;
                    return clampPercent((int) Math.round((double) usedPhysical * 100 / totalPhysical));
                }
            }
        } catch (Exception ignored) {
        }

        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return maxMemory <= 0 ? 0 : clampPercent((int) Math.round((double) usedMemory * 100 / maxMemory));
    }

    private int readDiskUsage() {
        try {
            int maxUsage = 0;
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                try {
                    if (store.isReadOnly()) {
                        continue;
                    }
                    long total = store.getTotalSpace();
                    if (total <= 0) {
                        continue;
                    }
                    long used = total - store.getUsableSpace();
                    int usage = clampPercent((int) Math.round((double) used * 100 / total));
                    if (usage > maxUsage) {
                        maxUsage = usage;
                    }
                } catch (Exception ignored) {
                }
            }
            return maxUsage;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int readNetworkLatency() {
        long start = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", serverPort), 1000);
            long end = System.nanoTime();
            return (int) Math.max(1, Math.round((end - start) / 1_000_000.0));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
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
