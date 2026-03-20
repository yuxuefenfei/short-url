package com.example.shorturl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson配置类
 * <p>
 * 模块职责：
 * - 配置全局的ObjectMapper bean
 * - 注册JavaTimeModule支持Java 8日期时间类型
 * <p>
 * 使用场景：
 * - 所有JSON序列化和反序列化操作
 * - 与Spring Boot默认配置集成
 * <p>
 * 依赖关系：
 * - 被Spring Boot自动配置使用
 * - 与ApiResponse配合统一响应格式
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
