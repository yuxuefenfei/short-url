package com.example.shorturl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 短网址管理系统应用启动类
 * <p>
 * 模块职责：
 * - 应用程序入口点
 * - 配置Spring Boot应用基础设置
 * - 启用MyBatis-Flex、异步处理、事务管理
 * <p>
 * 主要注解说明：
 * - @SpringBootApplication: Spring Boot自动配置
 * - @EnableAsync: 启用异步方法执行
 * - @EnableTransactionManagement: 启用事务管理
 * <p>
 * 依赖关系：
 * - 无外部依赖，作为应用程序根节点
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@MapperScan("com.example.shorturl.dao")
public class ShortUrlApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortUrlApplication.class, args);
    }
}