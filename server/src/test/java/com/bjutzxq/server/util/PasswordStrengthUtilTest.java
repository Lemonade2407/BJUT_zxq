package com.bjutzxq.server.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码强度工具类单元测试
 */
@DisplayName("密码强度工具测试")
class PasswordStrengthUtilTest {

    @Test
    @DisplayName("弱密码 - 纯数字")
    void testWeakPassword_NumbersOnly() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("123456");

        // Assert
        assertNotNull(result);
        assertEquals(PasswordStrengthUtil.StrengthLevel.WEAK, result.level());
        assertFalse(PasswordStrengthUtil.meetsMinimumRequirement("123456"));
    }

    @Test
    @DisplayName("弱密码 - 纯字母")
    void testWeakPassword_LettersOnly() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("abcdef");

        // Assert
        assertNotNull(result);
        assertEquals(PasswordStrengthUtil.StrengthLevel.WEAK, result.level());
    }

    @Test
    @DisplayName("中等密码 - 字母+数字")
    void testMediumPassword_LettersAndNumbers() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("abc123");

        // Assert
        assertNotNull(result);
        assertTrue(result.level().ordinal() >= PasswordStrengthUtil.StrengthLevel.MEDIUM.ordinal());
    }

    @Test
    @DisplayName("强密码 - 大小写字母+数字")
    void testStrongPassword_MixedCaseAndNumbers() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("Abc123456");

        // Assert
        assertNotNull(result);
        assertTrue(result.level().ordinal() >= PasswordStrengthUtil.StrengthLevel.STRONG.ordinal());
    }

    @Test
    @DisplayName("很强密码 - 包含特殊字符")
    void testVeryStrongPassword_WithSpecialChars() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("Abc@123456");

        // Assert
        assertNotNull(result);
        assertEquals(PasswordStrengthUtil.StrengthLevel.VERY_STRONG, result.level());
    }

    @Test
    @DisplayName("密码太短 - 少于6位")
    void testPassword_TooShort() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("Ab1");

        // Assert
        assertNotNull(result);
        assertEquals(PasswordStrengthUtil.StrengthLevel.WEAK, result.level());
        assertFalse(PasswordStrengthUtil.meetsMinimumRequirement("Ab1"));
    }

    @Test
    @DisplayName("空密码")
    void testEmptyPassword() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.score());
    }

    @Test
    @DisplayName("null 密码")
    void testNullPassword() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordStrengthUtil.evaluatePassword(null);
        });
    }

    @Test
    @DisplayName("满足最低要求 - 字母+数字")
    void testMeetsMinimumRequirement_Valid() {
        // Act & Assert
        assertTrue(PasswordStrengthUtil.meetsMinimumRequirement("Abc123"));
        assertTrue(PasswordStrengthUtil.meetsMinimumRequirement("Test123456"));
    }

    @Test
    @DisplayName("不满足最低要求 - 纯数字")
    void testMeetsMinimumRequirement_NumbersOnly() {
        // Act & Assert
        assertFalse(PasswordStrengthUtil.meetsMinimumRequirement("123456"));
    }

    @Test
    @DisplayName("不满足最低要求 - 纯字母")
    void testMeetsMinimumRequirement_LettersOnly() {
        // Act & Assert
        assertFalse(PasswordStrengthUtil.meetsMinimumRequirement("abcdef"));
    }

    @Test
    @DisplayName("不满足最低要求 - 太短")
    void testMeetsMinimumRequirement_TooShort() {
        // Act & Assert
        assertFalse(PasswordStrengthUtil.meetsMinimumRequirement("Ab1"));
    }

    @Test
    @DisplayName("密码强度评分 - 长度加分")
    void testPasswordScore_LengthBonus() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo shortResult = 
            PasswordStrengthUtil.evaluatePassword("Ab1");
        PasswordStrengthUtil.PasswordStrengthInfo longResult = 
            PasswordStrengthUtil.evaluatePassword("Abcdef123456");

        // Assert
        assertTrue(longResult.score() > shortResult.score(),
            "长密码应该获得更高分");
    }

    @Test
    @DisplayName("密码强度评分 - 多样性加分")
    void testPasswordScore_DiversityBonus() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo simpleResult = 
            PasswordStrengthUtil.evaluatePassword("Abc123");
        PasswordStrengthUtil.PasswordStrengthInfo complexResult = 
            PasswordStrengthUtil.evaluatePassword("Abc@123!#$");

        // Assert
        assertTrue(complexResult.score() > simpleResult.score(),
            "包含特殊字符的密码应该获得更高分");
    }

    @Test
    @DisplayName("密码强度描述 - 不为空")
    void testPasswordStrengthDescription_NotEmpty() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("Test123456");

        // Assert
        assertNotNull(result.getLevelDescription());
        assertFalse(result.getLevelDescription().isEmpty());
    }

    @Test
    @DisplayName("密码建议 - 弱密码有建议")
    void testPasswordSuggestion_WeakPassword() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("123456");

        // Assert
        assertNotNull(result.suggestion());
        assertFalse(result.suggestion().isEmpty());
    }

    @Test
    @DisplayName("常见弱密码检测")
    void testCommonWeakPasswords() {
        // Arrange
        String[] weakPasswords = {"password", "123456", "qwerty", "admin"};

        // Act & Assert
        for (String password : weakPasswords) {
            PasswordStrengthUtil.PasswordStrengthInfo result = 
                PasswordStrengthUtil.evaluatePassword(password);
            assertEquals(PasswordStrengthUtil.StrengthLevel.WEAK, result.level(),
                "密码 '" + password + "' 应该被识别为弱密码");
        }
    }

    @Test
    @DisplayName("边界长度密码 - 6位")
    void testPasswordBoundaryLength_SixChars() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("Ab1234");

        // Assert
        assertNotNull(result);
        assertTrue(PasswordStrengthUtil.meetsMinimumRequirement("Ab1234"));
    }

    @Test
    @DisplayName("边界长度密码 - 20位")
    void testPasswordBoundaryLength_TwentyChars() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("Abcdefghij1234567890");

        // Assert
        assertNotNull(result);
        assertTrue(PasswordStrengthUtil.meetsMinimumRequirement("Abcdefghij1234567890"));
    }

    @Test
    @DisplayName("超长密码 - 超过20位")
    void testPasswordTooLong() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("Abcdefghij1234567890XYZ");

        // Assert
        assertNotNull(result);
        // 超长密码可能仍然有效，但可能有其他限制
    }

    @Test
    @DisplayName("只包含空格")
    void testPasswordOnlySpaces() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("      ");

        // Assert
        assertNotNull(result);
        assertEquals(PasswordStrengthUtil.StrengthLevel.WEAK, result.level());
    }

    @Test
    @DisplayName("包含中文字符")
    void testPasswordWithChineseChars() {
        // Act
        PasswordStrengthUtil.PasswordStrengthInfo result = 
            PasswordStrengthUtil.evaluatePassword("密码Test123");

        // Assert
        assertNotNull(result);
        // 中文字符可能被识别为特殊字符
    }
}
