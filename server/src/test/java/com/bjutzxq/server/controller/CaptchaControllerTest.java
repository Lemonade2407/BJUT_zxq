package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.server.util.CaptchaUtil;
import com.bjutzxq.server.util.PasswordStrengthUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("验证码控制器测试")
class CaptchaControllerTest {

    @InjectMocks
    private CaptchaController captchaController;

    // ==================== getCaptcha ====================

    @Test
    @DisplayName("获取验证码成功 - 正常流程")
    void getCaptcha_Success() {
        // Arrange
        try (MockedStatic<CaptchaUtil> captchaUtilMock = mockStatic(CaptchaUtil.class)) {
            captchaUtilMock.when(() -> CaptchaUtil.generateCaptcha(anyString()))
                    .thenReturn("base64encodedimage");

            // Act
            Result<Map<String, String>> result = captchaController.getCaptcha();

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals("验证码获取成功", result.getMessage());
            assertNotNull(result.getData());
            assertNotNull(result.getData().get("sessionId"));
            assertTrue(result.getData().get("image").startsWith("data:image/png;base64,"));
        }
    }

    // ==================== checkPasswordStrength ====================

    @Test
    @DisplayName("评估密码强度成功 - 正常流程")
    void checkPasswordStrength_Success() {
        // Arrange
        Map<String, String> params = Map.of("password", "StrongP@ss123");

        try (MockedStatic<PasswordStrengthUtil> psuMock = mockStatic(PasswordStrengthUtil.class)) {
            psuMock.when(() -> PasswordStrengthUtil.evaluatePassword("StrongP@ss123"))
                    .thenReturn(new PasswordStrengthUtil.PasswordStrengthInfo(
                            PasswordStrengthUtil.StrengthLevel.STRONG, 85, "密码强度很好"));
            psuMock.when(() -> PasswordStrengthUtil.meetsMinimumRequirement("StrongP@ss123"))
                    .thenReturn(true);

            // Act
            Result<Map<String, Object>> result = captchaController.checkPasswordStrength(params);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertEquals("STRONG", result.getData().get("level"));
            assertEquals(85, result.getData().get("score"));
            assertEquals(true, result.getData().get("meetsRequirement"));
        }
    }

    @Test
    @DisplayName("评估密码强度失败 - 密码为null")
    void checkPasswordStrength_NullPassword() {
        // Arrange
        Map<String, String> params = Map.of();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> captchaController.checkPasswordStrength(params));
        assertEquals("密码不能为空", exception.getMessage());
    }
}
