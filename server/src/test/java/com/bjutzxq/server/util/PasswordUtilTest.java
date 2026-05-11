package com.bjutzxq.server.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("密码加密工具测试")
class PasswordUtilTest {

    // ==================== encode ====================

    @Test
    @DisplayName("加密密码 - 正常流程")
    void encode_Success() {
        // Act
        String encoded = PasswordUtil.encode("Test123456");

        // Assert
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$") || encoded.startsWith("$2y$"));
    }

    @Test
    @DisplayName("加密密码 - 不同密码生成不同密文")
    void encode_DifferentPasswords() {
        // Act
        String encoded1 = PasswordUtil.encode("Password123");
        String encoded2 = PasswordUtil.encode("Password456");

        // Assert
        assertNotEquals(encoded1, encoded2);
    }

    @Test
    @DisplayName("加密密码失败 - 密码为空")
    void encode_Empty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encode(""));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encode(null));
    }

    // ==================== matches ====================

    @Test
    @DisplayName("验证密码 - 正确密码返回true")
    void matches_Correct() {
        // Arrange
        String rawPassword = "Test123456";
        String encoded = PasswordUtil.encode(rawPassword);

        // Act
        boolean result = PasswordUtil.matches(rawPassword, encoded);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("验证密码 - 错误密码返回false")
    void matches_Wrong() {
        // Arrange
        String encoded = PasswordUtil.encode("CorrectPassword");

        // Act
        boolean result = PasswordUtil.matches("WrongPassword", encoded);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("验证密码 - null参数返回false")
    void matches_Null() {
        // Arrange
        String encoded = PasswordUtil.encode("Test123456");

        // Act
        boolean nullRaw = PasswordUtil.matches(null, encoded);
        boolean nullEncoded = PasswordUtil.matches("Test123456", null);
        boolean bothNull = PasswordUtil.matches(null, null);

        // Assert
        assertFalse(nullRaw);
        assertFalse(nullEncoded);
        assertFalse(bothNull);
    }

    @Test
    @DisplayName("验证密码 - 无效密文格式返回false")
    void matches_InvalidHash() {
        // Act
        boolean result = PasswordUtil.matches("password", "not-a-valid-bcrypt-hash");

        // Assert
        assertFalse(result);
    }
}
