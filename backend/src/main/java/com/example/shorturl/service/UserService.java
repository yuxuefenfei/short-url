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
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户注册。
     */
    @Transactional
    public User registerUser(String username, String password, String email, String role) {
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

        User existingUser = userDao.selectOneByQuery(
                QueryWrapper.create().where(UserTableDef.USER.USERNAME.eq(username))
        );
        if (existingUser != null) {
            throw new BusinessException(ResponseStatus.USERNAME_EXISTS);
        }

        if (StringUtils.hasText(email)) {
            User existingByEmail = userDao.selectOneByQuery(
                    QueryWrapper.create().where(UserTableDef.USER.EMAIL.eq(email))
            );
            if (existingByEmail != null) {
                throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "邮箱已被使用");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(StringUtils.hasText(role) ? role : "USER");
        user.setStatus(1);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());

        userDao.insert(user);
        log.info("用户注册成功: username={}, userId={}", username, user.getId());
        return user;
    }

    /**
     * 用户登录认证。
     */
    @Transactional
    public User authenticateUser(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "用户名和密码不能为空");
        }

        User user = userDao.selectOneByQuery(
                QueryWrapper.create().where(UserTableDef.USER.USERNAME.eq(username))
        );
        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResponseStatus.USER_DISABLED);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResponseStatus.PASSWORD_ERROR);
        }

        user.setLastLoginTime(LocalDateTime.now());
        userDao.update(user);
        return user;
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userDao.selectOneByQuery(
                QueryWrapper.create().where(UserTableDef.USER.USERNAME.eq(username))
        );
    }

    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return userDao.selectCountByQuery(
                QueryWrapper.create().where(UserTableDef.USER.USERNAME.eq(username))
        ) > 0;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userId == null ? null : userDao.selectOneById(userId);
    }

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
    }

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
        }
        return user;
    }

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
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResponseStatus.PASSWORD_ERROR);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedTime(LocalDateTime.now());
        userDao.update(user);
    }

    @Transactional
    public User resetPassword(Long userId, String newPassword) {
        if (userId == null) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "用户ID不能为空");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BusinessException(ResponseStatus.BAD_REQUEST.getCode(), "新密码长度不能少于6个字符");
        }

        User user = userDao.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ResponseStatus.USER_NOT_EXIST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedTime(LocalDateTime.now());
        userDao.update(user);
        return user;
    }

    @Transactional(readOnly = true)
    public List<User> getUserList(Integer page, Integer size, String keyword, String role, Integer status) {
        QueryWrapper queryWrapper = buildUserQuery(keyword, role, status);
        queryWrapper.orderBy(UserTableDef.USER.CREATED_TIME, false);
        return paginate(userDao.selectListByQuery(queryWrapper), page, size);
    }

    @Transactional(readOnly = true)
    public List<User> getUserList(Integer page, Integer size, String keyword) {
        return getUserList(page, size, keyword, null, null);
    }

    @Transactional(readOnly = true)
    public Long getUserCount(String keyword, String role, Integer status) {
        return userDao.selectCountByQuery(buildUserQuery(keyword, role, status));
    }

    @Transactional(readOnly = true)
    public Long getUserCount(String keyword) {
        return getUserCount(keyword, null, null);
    }

    @Transactional(readOnly = true)
    public boolean hasRole(Long userId, String role) {
        User user = getUserById(userId);
        return user != null && role.equals(user.getRole());
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(Long userId) {
        return hasRole(userId, "ADMIN");
    }

    @Transactional(readOnly = true)
    public boolean isUser(Long userId) {
        return hasRole(userId, "USER");
    }

    private QueryWrapper buildUserQuery(String keyword, String role, Integer status) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.where(UserTableDef.USER.USERNAME.like(keyword))
                    .or(UserTableDef.USER.EMAIL.like(keyword));
        }
        if (StringUtils.hasText(role)) {
            queryWrapper.and(UserTableDef.USER.ROLE.eq(role));
        }
        if (status != null) {
            queryWrapper.and(UserTableDef.USER.STATUS.eq(status));
        }

        return queryWrapper;
    }

    private List<User> paginate(List<User> users, Integer page, Integer size) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : size;
        int fromIndex = Math.max((safePage - 1) * safeSize, 0);
        if (fromIndex >= users.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(fromIndex + safeSize, users.size());
        return users.subList(fromIndex, toIndex);
    }
}
