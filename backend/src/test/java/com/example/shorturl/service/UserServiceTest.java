package com.example.shorturl.service;

import com.example.shorturl.common.exception.BusinessException;
import com.example.shorturl.common.response.ResponseStatus;
import com.example.shorturl.dao.UserDao;
import com.example.shorturl.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserService单元测试
 * <p>
 * 测试覆盖范围：
 * - 用户注册功能
 * - 用户登录验证
 * - 用户信息管理
 * - 密码修改功能
 * - 用户查询和权限检查
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
        testUser.setStatus(1);
        testUser.setCreatedTime(LocalDateTime.now());
        testUser.setUpdatedTime(LocalDateTime.now());
    }

    /**
     * 测试用户注册 - 正常场景
     */
    @Test
    void testRegisterUser_Success() {
        // Given
        String username = "newuser";
        String password = "password123";
        String email = "newuser@example.com";
        String role = "USER";

        when(userDao.selectOneByQuery(any())).thenReturn(null); // 用户名和邮箱都不存在
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        doNothing().when(userDao).insert(any(User.class));

        // When
        User result = userService.registerUser(username, password, email, role);

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(role, result.getRole());
        assertEquals(1, result.getStatus());
        verify(userDao).insert(any(User.class));
        verify(passwordEncoder).encode(password);
    }

    /**
     * 测试用户注册 - 用户名为空
     */
    @Test
    void testRegisterUser_EmptyUsername_ThrowsException() {
        // Given
        String emptyUsername = "";
        String password = "password123";

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.registerUser(emptyUsername, password, null, null));

        assertEquals("用户名不能为空", exception.getMessage());
        verify(userDao, never()).insert(any());
    }

    /**
     * 测试用户注册 - 密码为空
     */
    @Test
    void testRegisterUser_EmptyPassword_ThrowsException() {
        // Given
        String username = "newuser";
        String emptyPassword = "";

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.registerUser(username, emptyPassword, null, null));

        assertEquals("密码不能为空", exception.getMessage());
        verify(userDao, never()).insert(any());
    }

    /**
     * 测试用户注册 - 用户名过短
     */
    @Test
    void testRegisterUser_UsernameTooShort_ThrowsException() {
        // Given
        String shortUsername = "ab"; // 少于3个字符
        String password = "password123";

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.registerUser(shortUsername, password, null, null));

        assertEquals("用户名长度必须在3-50个字符之间", exception.getMessage());
        verify(userDao, never()).insert(any());
    }

    /**
     * 测试用户注册 - 密码过短
     */
    @Test
    void testRegisterUser_PasswordTooShort_ThrowsException() {
        // Given
        String username = "newuser";
        String shortPassword = "12345"; // 少于6个字符

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.registerUser(username, shortPassword, null, null));

        assertEquals("密码长度不能少于6个字符", exception.getMessage());
        verify(userDao, never()).insert(any());
    }

    /**
     * 测试用户注册 - 用户名已存在
     */
    @Test
    void testRegisterUser_UsernameExists_ThrowsException() {
        // Given
        String existingUsername = "existinguser";
        String password = "password123";

        when(userDao.selectOneByQuery(any())).thenReturn(testUser); // 用户名已存在

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.registerUser(existingUsername, password, null, null));

        assertEquals(ResponseStatus.USERNAME_EXISTS.getCode(), exception.getCode());
        verify(userDao, never()).insert(any());
    }

    /**
     * 测试用户注册 - 邮箱已存在
     */
    @Test
    void testRegisterUser_EmailExists_ThrowsException() {
        // Given
        String username = "newuser";
        String password = "password123";
        String existingEmail = "existing@example.com";

        // 用户名不存在，但邮箱存在
        when(userDao.selectOneByQuery(argThat(wrapper ->
                wrapper.toString().contains("username")))).thenReturn(null);
        when(userDao.selectOneByQuery(argThat(wrapper ->
                wrapper.toString().contains("email")))).thenReturn(testUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.registerUser(username, password, existingEmail, null));

        assertEquals("邮箱已被使用", exception.getMessage());
        verify(userDao, never()).insert(any());
    }

    /**
     * 测试用户登录 - 正常场景
     */
    @Test
    void testAuthenticateUser_Success() {
        // Given
        String username = "testuser";
        String password = "password123";

        when(userDao.selectOneByQuery(any())).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
        doNothing().when(userDao).update(any(User.class));

        // When
        User result = userService.authenticateUser(username, password);

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(userDao).update(any(User.class)); // 验证更新最后登录时间
    }

    /**
     * 测试用户登录 - 用户名或密码为空
     */
    @Test
    void testAuthenticateUser_EmptyCredentials_ThrowsException() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.authenticateUser("", ""));

        assertEquals("用户名和密码不能为空", exception.getMessage());
        verify(userDao, never()).selectOneByQuery(any());
    }

    /**
     * 测试用户登录 - 用户不存在
     */
    @Test
    void testAuthenticateUser_UserNotFound_ThrowsException() {
        // Given
        String nonExistentUsername = "nonexistent";
        String password = "password123";

        when(userDao.selectOneByQuery(any())).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.authenticateUser(nonExistentUsername, password));

        assertEquals(ResponseStatus.USER_NOT_EXIST.getCode(), exception.getCode());
    }

    /**
     * 测试用户登录 - 用户被禁用
     */
    @Test
    void testAuthenticateUser_UserDisabled_ThrowsException() {
        // Given
        String username = "disableduser";
        String password = "password123";

        User disabledUser = new User();
        disabledUser.setStatus(0); // 禁用状态

        when(userDao.selectOneByQuery(any())).thenReturn(disabledUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.authenticateUser(username, password));

        assertEquals(ResponseStatus.USER_DISABLED.getCode(), exception.getCode());
    }

    /**
     * 测试用户登录 - 密码错误
     */
    @Test
    void testAuthenticateUser_WrongPassword_ThrowsException() {
        // Given
        String username = "testuser";
        String wrongPassword = "wrongpassword";

        when(userDao.selectOneByQuery(any())).thenReturn(testUser);
        when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.authenticateUser(username, wrongPassword));

        assertEquals(ResponseStatus.PASSWORD_ERROR.getCode(), exception.getCode());
        verify(userDao, never()).update(any());
    }

    /**
     * 测试根据用户名获取用户 - 正常场景
     */
    @Test
    void testGetUserByUsername_Success() {
        // Given
        String username = "testuser";

        when(userDao.selectOneByQuery(any())).thenReturn(testUser);

        // When
        User result = userService.getUserByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(userDao).selectOneByQuery(any());
    }

    /**
     * 测试根据用户名获取用户 - 用户名为空
     */
    @Test
    void testGetUserByUsername_EmptyUsername_ReturnsNull() {
        // When
        User result = userService.getUserByUsername("");

        // Then
        assertNull(result);
        verify(userDao, never()).selectOneByQuery(any());
    }

    /**
     * 测试根据ID获取用户 - 正常场景
     */
    @Test
    void testGetUserById_Success() {
        // Given
        Long userId = 1L;

        when(userDao.selectOneById(userId)).thenReturn(testUser);

        // When
        User result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userDao).selectOneById(userId);
    }

    /**
     * 测试根据ID获取用户 - ID为空
     */
    @Test
    void testGetUserById_NullId_ReturnsNull() {
        // When
        User result = userService.getUserById(null);

        // Then
        assertNull(result);
        verify(userDao, never()).selectOneById(any());
    }

    /**
     * 测试更新用户状态 - 正常场景
     */
    @Test
    void testUpdateUserStatus_Success() {
        // Given
        Long userId = 1L;
        Integer newStatus = 0;

        when(userDao.selectOneById(userId)).thenReturn(testUser);
        doNothing().when(userDao).update(any(User.class));

        // When
        userService.updateUserStatus(userId, newStatus);

        // Then
        verify(userDao).update(any(User.class));
        verify(userDao).selectOneById(userId);
    }

    /**
     * 测试更新用户状态 - 参数为空
     */
    @Test
    void testUpdateUserStatus_NullParameters_ThrowsException() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateUserStatus(null, null));

        assertEquals("参数不能为空", exception.getMessage());
        verify(userDao, never()).update(any());
    }

    /**
     * 测试更新用户状态 - 用户不存在
     */
    @Test
    void testUpdateUserStatus_UserNotFound_ThrowsException() {
        // Given
        Long nonExistentUserId = 999L;
        Integer newStatus = 0;

        when(userDao.selectOneById(nonExistentUserId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateUserStatus(nonExistentUserId, newStatus));

        assertEquals(ResponseStatus.USER_NOT_EXIST.getCode(), exception.getCode());
        verify(userDao, never()).update(any());
    }

    /**
     * 测试修改用户密码 - 正常场景
     */
    @Test
    void testChangePassword_Success() {
        // Given
        Long userId = 1L;
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword123";

        when(userDao.selectOneById(userId)).thenReturn(testUser);
        when(passwordEncoder.matches(oldPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        doNothing().when(userDao).update(any(User.class));

        // When
        userService.changePassword(userId, oldPassword, newPassword);

        // Then
        verify(passwordEncoder).matches(oldPassword, testUser.getPassword());
        verify(passwordEncoder).encode(newPassword);
        verify(userDao).update(any(User.class));
    }

    /**
     * 测试修改用户密码 - 参数为空
     */
    @Test
    void testChangePassword_NullParameters_ThrowsException() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.changePassword(null, null, null));

        assertEquals("参数不能为空", exception.getMessage());
        verify(userDao, never()).update(any());
    }

    /**
     * 测试修改用户密码 - 新密码过短
     */
    @Test
    void testChangePassword_NewPasswordTooShort_ThrowsException() {
        // Given
        Long userId = 1L;
        String oldPassword = "oldPassword123";
        String shortNewPassword = "12345"; // 少于6个字符

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.changePassword(userId, oldPassword, shortNewPassword));

        assertEquals("新密码长度不能少于6个字符", exception.getMessage());
        verify(userDao, never()).update(any());
    }

    /**
     * 测试修改用户密码 - 旧密码错误
     */
    @Test
    void testChangePassword_WrongOldPassword_ThrowsException() {
        // Given
        Long userId = 1L;
        String wrongOldPassword = "wrongPassword";
        String newPassword = "newPassword123";

        when(userDao.selectOneById(userId)).thenReturn(testUser);
        when(passwordEncoder.matches(wrongOldPassword, testUser.getPassword())).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.changePassword(userId, wrongOldPassword, newPassword));

        assertEquals(ResponseStatus.PASSWORD_ERROR.getCode(), exception.getCode());
        verify(userDao, never()).update(any());
    }

    /**
     * 测试获取用户列表 - 正常场景
     */
    @Test
    void testGetUserList_Success() {
        // Given
        Integer page = 1;
        Integer size = 10;
        String keyword = "test";

        List<User> userList = Collections.singletonList(testUser);
        when(userDao.selectListByQuery(any())).thenReturn(userList);

        // When
        List<User> result = userService.getUserList(page, size, keyword);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userDao).selectListByQuery(any());
    }

    /**
     * 测试获取用户数量 - 正常场景
     */
    @Test
    void testGetUserCount_Success() {
        // Given
        String keyword = "test";
        Long expectedCount = 5L;

        when(userDao.selectCountByQuery(any())).thenReturn(expectedCount);

        // When
        Long result = userService.getUserCount(keyword);

        // Then
        assertEquals(expectedCount, result);
        verify(userDao).selectCountByQuery(any());
    }

    /**
     * 测试检查用户角色 - 用户有指定角色
     */
    @Test
    void testHasRole_UserHasRole_ReturnsTrue() {
        // Given
        Long userId = 1L;
        String role = "USER";

        when(userDao.selectOneById(userId)).thenReturn(testUser);

        // When
        boolean result = userService.hasRole(userId, role);

        // Then
        assertTrue(result);
        verify(userDao).selectOneById(userId);
    }

    /**
     * 测试检查用户角色 - 用户没有指定角色
     */
    @Test
    void testHasRole_UserDoesNotHaveRole_ReturnsFalse() {
        // Given
        Long userId = 1L;
        String role = "ADMIN";

        when(userDao.selectOneById(userId)).thenReturn(testUser);

        // When
        boolean result = userService.hasRole(userId, role);

        // Then
        assertFalse(result);
        verify(userDao).selectOneById(userId);
    }

    /**
     * 测试检查用户角色 - 用户不存在
     */
    @Test
    void testHasRole_UserNotFound_ReturnsFalse() {
        // Given
        Long nonExistentUserId = 999L;
        String role = "USER";

        when(userDao.selectOneById(nonExistentUserId)).thenReturn(null);

        // When
        boolean result = userService.hasRole(nonExistentUserId, role);

        // Then
        assertFalse(result);
        verify(userDao).selectOneById(nonExistentUserId);
    }

    /**
     * 测试检查用户是否为管理员
     */
    @Test
    void testIsAdmin_Success() {
        // Given
        Long adminUserId = 1L;
        User adminUser = new User();
        adminUser.setRole("ADMIN");

        when(userDao.selectOneById(adminUserId)).thenReturn(adminUser);

        // When
        boolean result = userService.isAdmin(adminUserId);

        // Then
        assertTrue(result);
    }

    /**
     * 测试检查用户是否为普通用户
     */
    @Test
    void testIsUser_Success() {
        // Given
        Long normalUserId = 1L;

        when(userDao.selectOneById(normalUserId)).thenReturn(testUser);

        // When
        boolean result = userService.isUser(normalUserId);

        // Then
        assertTrue(result);
    }
}