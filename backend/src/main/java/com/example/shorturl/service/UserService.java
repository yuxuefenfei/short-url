package com.example.shorturl.service;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.dao.UserDao;
import com.example.shorturl.model.entity.User;
import com.example.shorturl.model.entity.table.UserTableDef;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务类
 * <p>
 * 模块职责：
 * - 提供用户注册、登录、管理功能
 * - 处理用户密码加密和验证
 * - 管理用户状态和权限
 * - 提供用户信息查询服务
 * <p>
 * 核心功能：
 * - 用户注册和登录
 * - 密码加密和验证
 * - 用户状态管理
 * - 权限控制
 * <p>
 * 依赖关系：
 * - 被AuthService和UserController使用
 * - 依赖UserDao访问数据库
 * - 依赖PasswordEncoder进行密码加密
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /**
     * 获取用户DAO（仅供内部使用）
     */
    protected UserDao getUserDao() {
        return userDao;
    }

    /**
     * 用户注册
     */
    @Transactional
    public User registerUser(String username, String password, String email, String role) {
        // 参数验证
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "用户名不能为空");
        }

        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "密码不能为空");
        }

        if (username.length() < 3 || username.length() > 50) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "用户名长度必须在3-50个字符之间");
        }

        if (password.length() < 6) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "密码长度不能少于6个字符");
        }

        // 检查用户名是否已存在
        User existingUser = userDao.selectOneByQuery(
                QueryWrapper.create()
                        .where(UserTableDef.USER.USERNAME.eq(username))
        );

        if (existingUser != null) {
            throw new BusinessException(ResponseStatus.USERNAME_EXISTS);
        }

        // 检查邮箱是否已存在（如果提供了邮箱）
        if (StringUtils.hasText(email)) {
            User existingByEmail = userDao.selectOneByQuery(
                    QueryWrapper.create()
                            .where(UserTableDef.USER.EMAIL.eq(email))
            );

            if (existingByEmail != null) {
                throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "邮箱已被使用");
            }
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // BCrypt加密
        user.setEmail(email);
        user.setRole(StringUtils.hasText(role) ? role : "USER"); // 默认角色
        user.setStatus(1); // 正常状态
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());

        // 保存用户
        userDao.insert(user);

        log.info("用户注册成功: username={}, userId={}", username, user.getId());

        return user;
    }

    /**
     * 用户登录验证
     */
    @Transactional(readOnly = true)
    public User authenticateUser(String username, String password) {
        // 参数验证
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "用户名和密码不能为空");
        }

        // 查询用户
        User user = userDao.selectOneByQuery(
                QueryWrapper.create()
                        .where(UserTableDef.USER.USERNAME.eq(username))
        );

        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResponseStatus.USER_DISABLED);
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("用户密码错误: username={}", username);
            throw new BusinessException(ResponseStatus.PASSWORD_ERROR);
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userDao.update(user);

        log.info("用户登录成功: username={}, userId={}", username, user.getId());

        return user;
    }

    /**
     * 根据用户名获取用户
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }

        return userDao.selectOneByQuery(
                QueryWrapper.create()
                        .where(UserTableDef.USER.USERNAME.eq(username))
        );
    }

    /**
     * 根据ID获取用户
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        if (userId == null) {
            return null;
        }

        return userDao.selectOneById(userId);
    }

    /**
     * 更新用户状态
     */
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        if (userId == null || status == null) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "参数不能为空");
        }

        User user = userDao.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }

        user.setStatus(status);
        user.setUpdatedTime(LocalDateTime.now());

        userDao.update(user);

        log.info("用户状态更新: userId={}, status={}", userId, status);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUserInfo(Long userId, String email, String role) {
        if (userId == null) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "用户ID不能为空");
        }

        User user = userDao.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }

        boolean updated = false;

        if (StringUtils.hasText(email) && !email.equals(user.getEmail())) {
            // 检查邮箱是否已被其他用户使用
            User existingByEmail = userDao.selectOneByQuery(
                    QueryWrapper.create()
                            .where(UserTableDef.USER.EMAIL.eq(email))
                            .and(UserTableDef.USER.ID.ne(userId))
            );

            if (existingByEmail != null) {
                throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "邮箱已被使用");
            }

            user.setEmail(email);
            updated = true;
        }

        if (StringUtils.hasText(role) && !role.equals(user.getRole())) {
            user.setRole(role);
            updated = true;
        }

        if (updated) {
            user.setUpdatedTime(LocalDateTime.now());
            userDao.update(user);
            log.info("用户信息更新: userId={}", userId);
        }

        return user;
    }

    /**
     * 修改用户密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "参数不能为空");
        }

        if (newPassword.length() < 6) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "新密码长度不能少于6个字符");
        }

        User user = userDao.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResponseStatus.PASSWORD_ERROR);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedTime(LocalDateTime.now());

        userDao.update(user);

        log.info("用户密码修改: userId={}", userId);
    }

    /**
     * 获取用户列表（分页）
     */
    @Transactional(readOnly = true)
    public List<User> getUserList(Integer page, Integer size, String keyword) {
        com.mybatisflex.core.query.QueryWrapper queryWrapper =
                QueryWrapper.create();

        // 添加搜索条件
        if (StringUtils.hasText(keyword)) {
            queryWrapper.where(UserTableDef.USER.USERNAME.like(keyword))
                    .or(UserTableDef.USER.EMAIL.like(keyword));
        }

        // 按创建时间倒序
        queryWrapper.orderBy(UserTableDef.USER.CREATED_TIME, false);

        return userDao.selectListByQuery(queryWrapper);
    }

    /**
     * 获取用户数量
     */
    @Transactional(readOnly = true)
    public Long getUserCount(String keyword) {
        com.mybatisflex.core.query.QueryWrapper queryWrapper =
                QueryWrapper.create();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.where(UserTableDef.USER.USERNAME.like(keyword))
                    .or(UserTableDef.USER.EMAIL.like(keyword));
        }

        return userDao.selectCountByQuery(queryWrapper);
    }

    /**
     * 检查用户是否有指定角色
     */
    @Transactional(readOnly = true)
    public boolean hasRole(Long userId, String role) {
        User user = getUserById(userId);
        return user != null && role.equals(user.getRole());
    }

    /**
     * 检查用户是否为管理员
     */
    @Transactional(readOnly = true)
    public boolean isAdmin(Long userId) {
        return hasRole(userId, "ADMIN");
    }

    /**
     * 检查用户是否为普通用户
     */
    @Transactional(readOnly = true)
    public boolean isUser(Long userId) {
        return hasRole(userId, "USER");
    }
}