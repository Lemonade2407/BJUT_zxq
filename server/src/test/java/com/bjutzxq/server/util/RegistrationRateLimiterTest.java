package com.bjutzxq.server.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 注册频率限制器单元测试
 */
@DisplayName("注册频率限制器测试")
class RegistrationRateLimiterTest {

    @AfterEach
    void cleanup() {
        // 每个测试后清空记录，避免测试间相互影响
        RegistrationRateLimiter.clearAllRecords();
    }

    @Test
    @DisplayName("IP 频率限制 - 首次请求允许")
    void testIpLimit_FirstRequestAllowed() {
        // Act
        RegistrationRateLimiter.RateLimitResult result = 
            RegistrationRateLimiter.checkIpLimit("192.168.1.1");

        // Assert
        assertTrue(result.allowed());
        assertEquals("", result.message());
    }

    @Test
    @DisplayName("IP 频率限制 - 短时间内多次请求被限制")
    void testIpLimit_MultipleRequestsBlocked() {
        // Arrange & Act
        String ip = "192.168.1.100";
        
        // 前几次请求应该成功（限制是5次/小时）
        for (int i = 0; i < 5; i++) {
            RegistrationRateLimiter.RateLimitResult result = 
                RegistrationRateLimiter.checkIpLimit(ip);
            assertTrue(result.allowed(), "第 " + (i + 1) + " 次请求应该被允许");
        }

        // 超过限制后应该被阻止
        RegistrationRateLimiter.RateLimitResult blockedResult = 
            RegistrationRateLimiter.checkIpLimit(ip);
        
        // Assert
        assertFalse(blockedResult.allowed());
        assertNotNull(blockedResult.message());
        assertFalse(blockedResult.message().isEmpty());
    }

    @Test
    @DisplayName("邮箱频率限制 - 首次请求允许")
    void testEmailLimit_FirstRequestAllowed() {
        // Act
        RegistrationRateLimiter.RateLimitResult result = 
            RegistrationRateLimiter.checkEmailLimit("test@example.com");

        // Assert
        assertTrue(result.allowed());
        assertEquals("", result.message());
    }

    @Test
    @DisplayName("邮箱频率限制 - 同一邮箱多次请求被限制")
    void testEmailLimit_SameEmailMultipleRequestsBlocked() {
        // Arrange
        String email = "test@example.com";

        // Act - 多次请求（限制是3次/天）
        for (int i = 0; i < 3; i++) {
            RegistrationRateLimiter.RateLimitResult result = 
                RegistrationRateLimiter.checkEmailLimit(email);
            assertTrue(result.allowed(), "第 " + (i + 1) + " 次请求应该被允许");
        }

        // 超过限制
        RegistrationRateLimiter.RateLimitResult blockedResult = 
            RegistrationRateLimiter.checkEmailLimit(email);

        // Assert
        assertFalse(blockedResult.allowed());
        assertTrue(blockedResult.message().contains("邮箱"));
    }

    @Test
    @DisplayName("不同 IP - 互不影响")
    void testDifferentIps_Independent() {
        // Arrange
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";

        // Act
        RegistrationRateLimiter.RateLimitResult result1 = 
            RegistrationRateLimiter.checkIpLimit(ip1);
        RegistrationRateLimiter.RateLimitResult result2 = 
            RegistrationRateLimiter.checkIpLimit(ip2);

        // Assert
        assertTrue(result1.allowed());
        assertTrue(result2.allowed());
    }

    @Test
    @DisplayName("不同邮箱 - 互不影响")
    void testDifferentEmails_Independent() {
        // Arrange
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        // Act
        RegistrationRateLimiter.RateLimitResult result1 = 
            RegistrationRateLimiter.checkEmailLimit(email1);
        RegistrationRateLimiter.RateLimitResult result2 = 
            RegistrationRateLimiter.checkEmailLimit(email2);

        // Assert
        assertTrue(result1.allowed());
        assertTrue(result2.allowed());
    }

    @Test
    @DisplayName("空 IP 地址 - 处理")
    void testEmptyIpAddress() {
        // Act
        RegistrationRateLimiter.RateLimitResult result = 
            RegistrationRateLimiter.checkIpLimit("");

        // Assert
        assertNotNull(result);
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("null IP 地址 - 处理")
    void testNullIpAddress() {
        // Act
        RegistrationRateLimiter.RateLimitResult result = 
            RegistrationRateLimiter.checkIpLimit(null);

        // Assert
        assertNotNull(result);
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("空邮箱 - 处理")
    void testEmptyEmail() {
        // Act
        RegistrationRateLimiter.RateLimitResult result = 
            RegistrationRateLimiter.checkEmailLimit("");

        // Assert
        assertNotNull(result);
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("null 邮箱 - 处理")
    void testNullEmail() {
        // Act
        RegistrationRateLimiter.RateLimitResult result = 
            RegistrationRateLimiter.checkEmailLimit(null);

        // Assert
        assertNotNull(result);
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("RateLimitResult - 允许状态")
    void testRateLimitResult_Allowed() {
        // Act
        RegistrationRateLimiter.RateLimitResult result = 
            new RegistrationRateLimiter.RateLimitResult(true, "");

        // Assert
        assertTrue(result.allowed());
        assertEquals("", result.message());
    }

    @Test
    @DisplayName("RateLimitResult - 拒绝状态")
    void testRateLimitResult_Blocked() {
        // Act
        RegistrationRateLimiter.RateLimitResult result = 
            new RegistrationRateLimiter.RateLimitResult(false, "注册过于频繁");

        // Assert
        assertFalse(result.allowed());
        assertEquals("注册过于频繁", result.message());
    }

    @Test
    @DisplayName("边界情况 - 刚好达到限制")
    void testBoundary_AtLimit() {
        // Arrange
        String ip = "192.168.1.99";
        
        // 限制是 5 次，尝试 5 次
        for (int i = 0; i < 5; i++) {
            RegistrationRateLimiter.RateLimitResult result = 
                RegistrationRateLimiter.checkIpLimit(ip);
            assertTrue(result.allowed(), "第 " + (i + 1) + " 次应该被允许");
        }

        // 第 6 次应该被拒绝
        RegistrationRateLimiter.RateLimitResult result = 
            RegistrationRateLimiter.checkIpLimit(ip);
        
        // Assert
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("大量不同 IP - 性能测试")
    void testManyDifferentIps_Performance() {
        // Act & Assert
        for (int i = 0; i < 100; i++) {
            String ip = "192.168.2." + i;
            RegistrationRateLimiter.RateLimitResult result = 
                RegistrationRateLimiter.checkIpLimit(ip);
            assertTrue(result.allowed());
        }
    }

    @Test
    @DisplayName("大量不同邮箱 - 性能测试")
    void testManyDifferentEmails_Performance() {
        // Act & Assert
        for (int i = 0; i < 100; i++) {
            String email = "user" + i + "@test.com";
            RegistrationRateLimiter.RateLimitResult result = 
                RegistrationRateLimiter.checkEmailLimit(email);
            assertTrue(result.allowed());
        }
    }
}
