package com.bjutzxq.server.service;

import com.bjutzxq.common.BusinessException;
import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Role;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.pojo.vo.LoginVO;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.util.JwtUtil;
import com.bjutzxq.server.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("Test123456");
        testUser.setEmployeeId("20230101");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setSex("男");
        testUser.setBio("测试用户");
        testUser.setStatus(Constants.User.STATUS_NORMAL);
        testUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("注册成功 - 正常流程")
    void testRegister_Success() {
        // Arrange
        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        when(userMapper.selectByEmail("test@example.com")).thenReturn(null);
        when(userMapper.selectByEmployeeId("20230101")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1);
            return 1;
        });

        // Act
        User result = userService.register(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("20230101", result.getEmployeeId());
        assertEquals(Role.USER, result.getRole());
        assertEquals(Constants.User.STATUS_NORMAL, result.getStatus());

        // 验证密码已加密（BCrypt）
        assertNotEquals("Test123456", result.getPassword());
        assertTrue(result.getPassword().startsWith("$2"));

        verify(userMapper).selectByUsername("testuser");
        verify(userMapper).selectByEmail("test@example.com");
        verify(userMapper).selectByEmployeeId("20230101");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("注册成功 - 设置默认头像")
    void testRegister_DefaultAvatar() {
        // Arrange
        testUser.setAvatar(null);
        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        when(userMapper.selectByEmail("test@example.com")).thenReturn(null);
        when(userMapper.selectByEmployeeId("20230101")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1);
            return 1;
        });

        // Act
        User result = userService.register(testUser);

        // Assert
        assertNotNull(result);
        assertEquals("/logo.svg", result.getAvatar(), "应该设置默认头像");
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void testRegister_UsernameExists() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(999);
        existingUser.setUsername("testuser");
        when(userMapper.selectByUsername("testuser")).thenReturn(existingUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.register(testUser));

        assertEquals(409, exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册失败 - 邮箱已存在")
    void testRegister_EmailExists() {
        // Arrange
        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        User existingUser = new User();
        existingUser.setId(999);
        existingUser.setEmail("test@example.com");
        when(userMapper.selectByEmail("test@example.com")).thenReturn(existingUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.register(testUser));

        assertEquals(409, exception.getCode());
        assertEquals("邮箱已被使用", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册失败 - 身份标识号已存在")
    void testRegister_EmployeeIdExists() {
        // Arrange
        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        User existingUser = new User();
        existingUser.setId(999);
        existingUser.setEmployeeId("20230101");
        when(userMapper.selectByEmployeeId("20230101")).thenReturn(existingUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.register(testUser));

        assertEquals(409, exception.getCode());
        assertEquals("身份标识号已被使用", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("登录成功 - 使用用户名")
    void testLogin_Success_WithUsername() {
        // Arrange
        testUser.setId(1);
        testUser.setPassword(PasswordUtil.encode("Test123456"));
        testUser.setAvatar("http://example.com/avatar.jpg");

        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(() -> JwtUtil.generateTokenPair(anyInt()))
                .thenReturn(new JwtUtil.TokenPair("mock-access-token", "mock-refresh-token", 900L));

            // Act
            LoginVO result = userService.login("testuser", "Test123456");

            // Assert
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            assertEquals(1, result.getId());
            assertEquals("testuser", result.getUsername());
            verify(userMapper).selectByUsername("testuser");
        }
    }

    @Test
    @DisplayName("登录成功 - 使用邮箱")
    void testLogin_Success_WithEmail() {
        // Arrange
        testUser.setId(1);
        testUser.setPassword(PasswordUtil.encode("Test123456"));

        when(userMapper.selectByUsername("test@example.com")).thenReturn(null);
        when(userMapper.selectByEmail("test@example.com")).thenReturn(testUser);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(() -> JwtUtil.generateTokenPair(anyInt()))
                .thenReturn(new JwtUtil.TokenPair("mock-access-token", "mock-refresh-token", 900L));

            // Act
            LoginVO result = userService.login("test@example.com", "Test123456");

            // Assert
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            verify(userMapper).selectByEmail("test@example.com");
        }
    }

    @Test
    @DisplayName("登录成功 - 使用身份标识号")
    void testLogin_Success_WithEmployeeId() {
        // Arrange
        testUser.setId(1);
        testUser.setPassword(PasswordUtil.encode("Test123456"));

        when(userMapper.selectByUsername("20230101")).thenReturn(null);
        when(userMapper.selectByEmail("20230101")).thenReturn(null);
        when(userMapper.selectByEmployeeId("20230101")).thenReturn(testUser);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            jwtUtilMock.when(() -> JwtUtil.generateTokenPair(anyInt()))
                .thenReturn(new JwtUtil.TokenPair("mock-access-token", "mock-refresh-token", 900L));

            // Act
            LoginVO result = userService.login("20230101", "Test123456");

            // Assert
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            verify(userMapper).selectByEmployeeId("20230101");
        }
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void testLogin_UserNotFound() {
        // Arrange
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);
        when(userMapper.selectByEmail("nonexistent")).thenReturn(null);
        when(userMapper.selectByEmployeeId("nonexistent")).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.login("nonexistent", "password"));

        assertEquals(404, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_WrongPassword() {
        // Arrange
        testUser.setId(1);
        testUser.setPassword(PasswordUtil.encode("Test123456"));
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.login("testuser", "WrongPassword"));

        assertEquals(401, exception.getCode());
        assertEquals("密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 账号已被禁用")
    void testLogin_AccountDisabled() {
        // Arrange
        testUser.setId(1);
        testUser.setStatus(Constants.User.STATUS_DISABLED);
        testUser.setPassword(PasswordUtil.encode("Test123456"));
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.login("testuser", "Test123456"));

        assertEquals(403, exception.getCode());
        assertEquals("账号已被禁用", exception.getMessage());
    }

    @Test
    @DisplayName("获取当前用户信息 - 成功")
    void testGetCurrentUser_Success() {
        // Arrange
        testUser.setId(1);
        testUser.setPassword("encrypted");
        when(userMapper.selectById(1)).thenReturn(testUser);

        // Act
        User result = userService.getCurrentUser(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertNull(result.getPassword(), "密码应该被清除");
        verify(userMapper).selectById(1);
    }

    @Test
    @DisplayName("获取当前用户信息 - 用户不存在")
    void testGetCurrentUser_UserNotFound() {
        // Arrange
        when(userMapper.selectById(999)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.getCurrentUser(999));

        assertEquals(404, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());
    }
}
