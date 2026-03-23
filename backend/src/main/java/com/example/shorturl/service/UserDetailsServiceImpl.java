package com.example.shorturl.service;

import com.example.shorturl.dao.UserDao;
import com.example.shorturl.model.entity.User;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * UserDetailsService实现类
 * <p>
 * 模块职责：
 * - 实现Spring Security的用户详情服务
 * - 从数据库加载用户信息
 * - 转换用户实体为UserDetails对象
 * - 处理用户不存在等异常情况
 * <p>
 * Spring Security集成：
 * - 实现UserDetailsService接口
 * - 提供loadUserByUsername方法
 * - 返回UserDetails对象用于认证
 * <p>
 * 依赖关系：
 * - 被JwtAuthenticationFilter使用
 * - 依赖UserDao访问数据库
 * - 与User实体配合使用
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserDao userDao;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户信息: username={}", username);

        // 从数据库查询用户
        User user = userDao.selectOneByQuery(com.mybatisflex.core.query.QueryWrapper.create().eq("username", username));

        if (user == null) {
            log.warn("用户不存在: username={}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("用户已被禁用: username={}", username);
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 转换为用户详情对象
        return createUserDetails(user);
    }

    /**
     * 创建UserDetails对象
     */
    private UserDetails createUserDetails(User user) {
        // 构建权限列表
        String role = user.getRole();
        if (role == null || role.isEmpty()) {
            role = "USER"; // 默认角色
        }

        // 将角色转换为权限格式 (ROLE_USER, ROLE_ADMIN)
        String authority = "ROLE_" + role.toUpperCase();

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }

    /**
     * 根据用户ID加载用户详情
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        log.debug("根据ID加载用户信息: userId={}", userId);

        User user = userDao.selectOneById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            throw new UsernameNotFoundException("用户不存在，ID: " + userId);
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("用户已被禁用: userId={}", userId);
            throw new UsernameNotFoundException("用户已被禁用，ID: " + userId);
        }

        return createUserDetails(user);
    }

    /**
     * 检查用户是否存在且可用
     */
    @Transactional(readOnly = true)
    public boolean isUserValid(String username) {
        try {
            UserDetails userDetails = loadUserByUsername(username);
            return userDetails.isEnabled() && userDetails.isAccountNonExpired();
        } catch (UsernameNotFoundException e) {
            return false;
        }
    }

    /**
     * 获取用户角色
     */
    @Transactional(readOnly = true)
    public String getUserRole(String username) {
        User user = userDao.selectOneByQuery(
                QueryWrapper.create()
                        .eq("username", username)
        );

        return Optional.ofNullable(user)
                .map(User::getRole)
                .orElse(null);
    }

    /**
     * 验证用户密码
     */
    @Transactional(readOnly = true)
    public boolean validatePassword(String username, String rawPassword) {
        try {
            UserDetails userDetails = loadUserByUsername(username);
            // 密码验证由Spring Security的PasswordEncoder处理
            // 这里只做用户是否存在的基本验证
            return userDetails != null;
        } catch (UsernameNotFoundException e) {
            return false;
        }
    }
}
