package com.example.shorturl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置类
 * <p>
 * 模块职责：
 * - 配置异步任务执行器
 * - 管理线程池参数
 * - 提供日志任务专用执行器
 * <p>
 * 核心功能：
 * - 日志异步写入
 * - 邮件发送
 * - 文件处理
 * - 其他耗时操作
 * <p>
 * 依赖关系：
 * - 被AsyncLogService使用
 * - 与其他异步服务配合
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 日志任务专用线程池
     * 用于异步保存操作日志和访问日志
     */
    @Bean("logTaskExecutor")
    public Executor logTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：CPU核心数
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize);

        // 最大线程数：核心线程数的2倍
        executor.setMaxPoolSize(corePoolSize * 2);

        // 队列容量：1000
        executor.setQueueCapacity(1000);

        // 线程空闲时间：60秒
        executor.setKeepAliveSeconds(60);

        // 线程名称前缀
        executor.setThreadNamePrefix("LogTask-");

        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：60秒
        executor.setAwaitTerminationSeconds(60);

        // 初始化
        executor.initialize();

        return executor;
    }

    /**
     * 通用异步任务线程池
     * 用于其他异步操作
     */
    @Bean("commonTaskExecutor")
    public Executor commonTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：CPU核心数的一半
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        executor.setCorePoolSize(corePoolSize);

        // 最大线程数：核心线程数的3倍
        executor.setMaxPoolSize(corePoolSize * 3);

        // 队列容量：500
        executor.setQueueCapacity(500);

        // 线程空闲时间：30秒
        executor.setKeepAliveSeconds(30);

        // 线程名称前缀
        executor.setThreadNamePrefix("CommonTask-");

        // 拒绝策略：丢弃最旧的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        // 等待所有任务完成后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：30秒
        executor.setAwaitTerminationSeconds(30);

        // 初始化
        executor.initialize();

        return executor;
    }

    /**
     * IO密集型任务线程池
     * 用于文件处理、网络请求等IO密集型操作
     */
    @Bean("ioTaskExecutor")
    public Executor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：CPU核心数的2倍
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        executor.setCorePoolSize(corePoolSize);

        // 最大线程数：核心线程数的4倍
        executor.setMaxPoolSize(corePoolSize * 2);

        // 队列容量：2000
        executor.setQueueCapacity(2000);

        // 线程空闲时间：120秒
        executor.setKeepAliveSeconds(120);

        // 线程名称前缀
        executor.setThreadNamePrefix("IoTask-");

        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：120秒
        executor.setAwaitTerminationSeconds(120);

        // 初始化
        executor.initialize();

        return executor;
    }
}