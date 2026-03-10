package com.example.shorturl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置类
 * <p>
 * 模块职责：
 * - 配置BCrypt密码加密器
 * - 提供统一的密码加密和验证服务
 * <p>
 * BCrypt特性：
 * - 自适应哈希算法
 * - 内置盐值生成
 * - 可配置的加密强度
 * - 防止彩虹表攻击
 * <p>
 * 依赖关系：
 * - 被UserService和AuthService使用
 * - 与Spring Security集成
 */
@Configuration
public class PasswordConfig {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String password = "admin123";
        String encoded = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("Encoded:  " + encoded);
        System.out.println("Strength: 12");

        // 验证
        System.out.println("Matches: " + encoder.matches(password, encoded));
    }

    /**
     * BCrypt密码加密器
     *
     * @return PasswordEncoder实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength = 12 (默认值，可调整加密强度)
        // 强度范围：4-31，值越大加密越强但性能越低
        return new BCryptPasswordEncoder(12);
    }
}