package com.example.shorturl.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 性能监控配置
 * <p>
 * 模块职责：
 * - 配置应用性能监控
 * - 集成Prometheus指标收集
 * - 监控系统资源使用
 * - 提供性能指标数据
 * <p>
 * 核心功能：
 * - JVM监控指标
 * - 系统资源监控
 * - HTTP请求监控
 * - 自定义业务指标
 * <p>
 * 依赖关系：
 * - 被Spring Actuator使用
 * - 提供Prometheus格式指标
 * - 支持监控面板集成
 */
@Configuration
public class MonitoringConfig {

    /**
     * Prometheus注册表
     */
    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    /**
     * 计时切面配置
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * 通用指标注册表定制器
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                "application", "short-url-service",
                "environment", "production"
        );
    }

    /**
     * JVM内存监控
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    /**
     * JVM GC监控
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    /**
     * JVM线程监控
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    /**
     * JVM类加载监控
     */
    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }


    /**
     * 系统处理器监控
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }

    /**
     * 磁盘空间监控
     */
    @Bean("customDiskSpaceMetrics")
    public DiskSpaceMetrics customDiskSpaceMetrics() {
        return new DiskSpaceMetrics(new File("."));
    }

    /**
     * 数据库连接池监控
     */
    @Bean
    public ConnectionPoolMetrics connectionPoolMetrics() {
        return new ConnectionPoolMetrics();
    }

    /**
     * 自定义连接池监控类
     */
    public static class ConnectionPoolMetrics {
        // 这里可以添加自定义的连接池监控逻辑
        // 由于HikariCP的监控通常通过Actuator自动配置
        // 这里提供一个占位符供后续扩展
    }
}
