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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

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

    @Test
    void testRegisterUserSuccess() {
        when(userDao.selectOneByQuery(any())).thenReturn(null, null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userDao.insert(any(User.class))).thenReturn(1);

        User result = userService.registerUser("newuser", "password123", "new@example.com", "USER");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(userDao).insert(any(User.class));
    }

    @Test
    void testRegisterUserUsernameExists() {
        when(userDao.selectOneByQuery(any())).thenReturn(testUser);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.registerUser("testuser", "password123", null, null)
        );

        assertEquals(ResponseStatus.USERNAME_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void testAuthenticateUserSuccess() {
        when(userDao.selectOneByQuery(any())).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(userDao.update(any(User.class))).thenReturn(1);

        User result = userService.authenticateUser("testuser", "password123");

        assertEquals("testuser", result.getUsername());
        verify(userDao).update(any(User.class));
    }

    @Test
    void testAuthenticateUserWrongPassword() {
        when(userDao.selectOneByQuery(any())).thenReturn(testUser);
        when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.authenticateUser("testuser", "wrong")
        );

        assertEquals(ResponseStatus.PASSWORD_ERROR.getCode(), exception.getCode());
        verify(userDao, never()).update(any(User.class));
    }

    @Test
    void testUpdateUserStatusSuccess() {
        when(userDao.selectOneById(1L)).thenReturn(testUser);
        when(userDao.update(any(User.class))).thenReturn(1);

        userService.updateUserStatus(1L, 0);

        verify(userDao).update(any(User.class));
    }

    @Test
    void testUpdateUserInfoSuccess() {
        when(userDao.selectOneById(1L)).thenReturn(testUser);
        when(userDao.selectOneByQuery(any())).thenReturn(null);
        when(userDao.update(any(User.class))).thenReturn(1);

        User updated = userService.updateUserInfo(1L, "new@example.com", "ADMIN");

        assertEquals("new@example.com", updated.getEmail());
        assertEquals("ADMIN", updated.getRole());
    }

    @Test
    void testChangePasswordSuccess() {
        when(userDao.selectOneById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userDao.update(any(User.class))).thenReturn(1);

        userService.changePassword(1L, "oldPassword", "newPassword");

        verify(userDao).update(any(User.class));
    }

    @Test
    void testResetPasswordSuccess() {
        when(userDao.selectOneById(1L)).thenReturn(testUser);
        when(passwordEncoder.encode("Reset@123")).thenReturn("encodedReset");
        when(userDao.update(any(User.class))).thenReturn(1);

        User user = userService.resetPassword(1L, "Reset@123");

        assertNotNull(user);
        verify(userDao).update(any(User.class));
    }

    @Test
    void testGetUserListSuccess() {
        when(userDao.selectListByQuery(any())).thenReturn(List.of(testUser));

        List<User> users = userService.getUserList(1, 10, "test");

        assertEquals(1, users.size());
    }

    @Test
    void testGetUserCountSuccess() {
        when(userDao.selectCountByQuery(any())).thenReturn(5L);

        Long count = userService.getUserCount("test");

        assertEquals(5L, count);
    }

    @Test
    void testHasRoleAndAdminChecks() {
        when(userDao.selectOneById(1L)).thenReturn(testUser);

        assertTrue(userService.hasRole(1L, "USER"));
        assertFalse(userService.isAdmin(1L));
    }
}
