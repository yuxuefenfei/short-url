package com.example.shorturl.config;

import com.mybatisflex.spring.boot.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 配置类
 * <p>
 * 模块职责：
 * - 配置MyBatis-Flex核心设置
 * - 自定义MyBatis配置
 * - 确保DAO接口正确扫描和初始化
 * <p>
 * 配置说明：
 * - 使用@ConfigurationCustomizer自定义MyBatis配置
 * - 配合application.yml中的mybatis-flex配置
 * - 确保SqlSessionFactory正确注入到DAO
 * <p>
 * 依赖关系：
 * - 被Spring Boot自动配置加载
 * - 为所有DAO接口提供基础配置
 */
@Configuration
public class MyBatisFlexConfig {

    /**
     * MyBatis配置自定义器
     * 用于自定义MyBatis-Flex的配置
     */
    @Bean
    public ConfigurationCustomizer mybatisFlexCustomizer() {
        return configuration -> {
            // 启用下划线转驼峰命名
            configuration.setMapUnderscoreToCamelCase(true);

            // 设置日志实现
            configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);

            // 其他MyBatis配置可以在这里添加
            // 例如：缓存配置、执行器类型等
        };
    }
}