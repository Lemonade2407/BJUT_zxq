package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.dto.RegisterDTO;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.pojo.vo.LoginVO;
import com.bjutzxq.server.service.UserService;
import com.bjutzxq.server.util.CaptchaUtil;
import com.bjutzxq.server.util.RegistrationRateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("认证控制器测试")
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletResponse mockResponse;

    @InjectMocks
    private AuthController authController;

    private RegisterDTO validRegisterRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterDTO();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setPassword("Test123456");
        validRegisterRequest.setConfirmPassword("Test123456");
        validRegisterRequest.setEmployeeId("20230101");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPhone("13800138000");
        validRegisterRequest.setSex("男");
        validRegisterRequest.setBio("测试用户");
        validRegisterRequest.setCaptchaSessionId("test-session-id");
        validRegisterRequest.setCaptchaCode("ABCD");

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setEmployeeId("20230101");
    }

    @Test
    @DisplayName("注册成功 - 正常流程")
    void testRegister_Success() {
        // Arrange
        try (MockedStatic<CaptchaUtil> captchaUtilMock = mockStatic(CaptchaUtil.class);
             MockedStatic<RegistrationRateLimiter> rateLimiterMock = mockStatic(RegistrationRateLimiter.class)) {

            captchaUtilMock.when(() -> CaptchaUtil.verifyCaptcha("test-session-id", "ABCD"))
                .thenReturn(true);

            RegistrationRateLimiter.RateLimitResult allowedResult =
                new RegistrationRateLimiter.RateLimitResult(true, "");
rateLimiterMock.when(() -> RegistrationRateLimiter.checkEmailLimit(anyString()))
                .thenReturn(allowedResult);

            when(userService.register(any(User.class))).thenReturn(testUser);

            // Act
            Result<User> result = authController.register(validRegisterRequest);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals("注册成功", result.getMessage());
            assertNotNull(result.getData());
            assertEquals("testuser", result.getData().getUsername());

            verify(userService).register(any(User.class));
        }
    }

    @Test
    @DisplayName("注册失败 - 缺少验证码")
    void testRegister_MissingCaptcha() {
        // Arrange
        validRegisterRequest.setCaptchaCode(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authController.register(validRegisterRequest));
        assertEquals("请输入图形验证码", exception.getMessage());
        verify(userService, never()).register(any(User.class));
    }

    @Test
    @DisplayName("注册失败 - 验证码错误")
    void testRegister_InvalidCaptcha() {
        // Arrange
        try (MockedStatic<CaptchaUtil> captchaUtilMock = mockStatic(CaptchaUtil.class)) {
            captchaUtilMock.when(() -> CaptchaUtil.verifyCaptcha("test-session-id", "ABCD"))
                .thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> authController.register(validRegisterRequest));
            assertEquals("验证码错误或已过期", exception.getMessage());
            verify(userService, never()).register(any(User.class));
        }
    }

    @Test
    @DisplayName("注册失败 - 两次密码不一致")
    void testRegister_PasswordMismatch() {
        // Arrange
        validRegisterRequest.setConfirmPassword("DifferentPassword");

        try (MockedStatic<CaptchaUtil> captchaUtilMock = mockStatic(CaptchaUtil.class);
             MockedStatic<RegistrationRateLimiter> rateLimiterMock = mockStatic(RegistrationRateLimiter.class)) {

            captchaUtilMock.when(() -> CaptchaUtil.verifyCaptcha(anyString(), anyString()))
                .thenReturn(true);

            RegistrationRateLimiter.RateLimitResult allowedResult =
                new RegistrationRateLimiter.RateLimitResult(true, "");
rateLimiterMock.when(() -> RegistrationRateLimiter.checkEmailLimit(anyString()))
                .thenReturn(allowedResult);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> authController.register(validRegisterRequest));
            assertEquals("两次输入的密码不一致", exception.getMessage());
            verify(userService, never()).register(any(User.class));
        }
    }

    @Test
    @DisplayName("注册失败 - 邮箱频率限制")
    void testRegister_EmailRateLimitExceeded() {
        // Arrange
        try (MockedStatic<CaptchaUtil> captchaUtilMock = mockStatic(CaptchaUtil.class);
             MockedStatic<RegistrationRateLimiter> rateLimiterMock = mockStatic(RegistrationRateLimiter.class)) {

            captchaUtilMock.when(() -> CaptchaUtil.verifyCaptcha(anyString(), anyString()))
                .thenReturn(true);

            RegistrationRateLimiter.RateLimitResult blockedResult =
                new RegistrationRateLimiter.RateLimitResult(false, "该邮箱注册过于频繁");
            rateLimiterMock.when(() -> RegistrationRateLimiter.checkEmailLimit(anyString()))
                .thenReturn(blockedResult);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> authController.register(validRegisterRequest));
            assertEquals("该邮箱注册过于频繁", exception.getMessage());
            verify(userService, never()).register(any(User.class));
        }
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void testRegister_UsernameExists() {
        // Arrange
        try (MockedStatic<CaptchaUtil> captchaUtilMock = mockStatic(CaptchaUtil.class);
             MockedStatic<RegistrationRateLimiter> rateLimiterMock = mockStatic(RegistrationRateLimiter.class)) {

            captchaUtilMock.when(() -> CaptchaUtil.verifyCaptcha(anyString(), anyString()))
                .thenReturn(true);

            RegistrationRateLimiter.RateLimitResult allowedResult =
                new RegistrationRateLimiter.RateLimitResult(true, "");
rateLimiterMock.when(() -> RegistrationRateLimiter.checkEmailLimit(anyString()))
                .thenReturn(allowedResult);

            when(userService.register(any(User.class)))
                .thenThrow(new RuntimeException("用户名已存在"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authController.register(validRegisterRequest));
            assertEquals("用户名已存在", exception.getMessage());
        }
    }

    @Test
    @DisplayName("登录成功 - 正常流程")
    void testLogin_Success() {
        // Arrange
        Map<String, String> loginParams = new HashMap<>();
        loginParams.put("username", "testuser");
        loginParams.put("password", "Test123456");

        LoginVO loginVO = new LoginVO();
        loginVO.setId(1);
        loginVO.setUsername("testuser");
        loginVO.setToken("mock-jwt-token");

        when(userService.login("testuser", "Test123456")).thenReturn(loginVO);

        // Act
        Result<LoginVO> result = authController.login(loginParams, mockResponse);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("登录成功", result.getMessage());
        assertNotNull(result.getData());
        assertEquals("mock-jwt-token", result.getData().getToken());
        verify(userService).login("testuser", "Test123456");
    }

    @Test
    @DisplayName("登录失败 - 用户名为空")
    void testLogin_EmptyUsername() {
        // Arrange
        Map<String, String> loginParams = new HashMap<>();
        loginParams.put("username", "");
        loginParams.put("password", "Test123456");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authController.login(loginParams, mockResponse));
        assertEquals("用户名不能为空", exception.getMessage());
        verify(userService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 密码为空")
    void testLogin_EmptyPassword() {
        // Arrange
        Map<String, String> loginParams = new HashMap<>();
        loginParams.put("username", "testuser");
        loginParams.put("password", "");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authController.login(loginParams, mockResponse));
        assertEquals("密码不能为空", exception.getMessage());
        verify(userService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void testLogin_UserNotFound() {
        // Arrange
        Map<String, String> loginParams = new HashMap<>();
        loginParams.put("username", "nonexistent");
        loginParams.put("password", "password");

        when(userService.login("nonexistent", "password"))
            .thenThrow(new RuntimeException("用户不存在"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authController.login(loginParams, mockResponse));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_WrongPassword() {
        // Arrange
        Map<String, String> loginParams = new HashMap<>();
        loginParams.put("username", "testuser");
        loginParams.put("password", "WrongPassword");

        when(userService.login("testuser", "WrongPassword"))
            .thenThrow(new RuntimeException("密码错误"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authController.login(loginParams, mockResponse));
        assertEquals("密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("退出登录 - 成功")
    void testLogout_Success() {
        // Act
        Result<Void> result = authController.logout(mockResponse);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("退出成功", result.getMessage());
    }
}
