package com.bjutzxq.server.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("注册频率限制器测试")
class RegistrationRateLimiterTest {

    @AfterEach
    void cleanup() {
        RegistrationRateLimiter.clearAllRecords();
    }

    @Test
    @DisplayName("邮箱频率限制 - 首次请求允许")
    void testEmailLimit_FirstRequestAllowed() {
        RegistrationRateLimiter.RateLimitResult result =
            RegistrationRateLimiter.checkEmailLimit("test@example.com");
        assertTrue(result.allowed());
        assertEquals("", result.message());
    }

    @Test
    @DisplayName("邮箱频率限制 - 同一邮箱多次请求被限制")
    void testEmailLimit_SameEmailMultipleRequestsBlocked() {
        String email = "test@example.com";
        for (int i = 0; i < 3; i++) {
            RegistrationRateLimiter.RateLimitResult result =
                RegistrationRateLimiter.checkEmailLimit(email);
            assertTrue(result.allowed(), "第 " + (i + 1) + " 次请求应该被允许");
        }
        RegistrationRateLimiter.RateLimitResult blockedResult =
            RegistrationRateLimiter.checkEmailLimit(email);
        assertFalse(blockedResult.allowed());
        assertTrue(blockedResult.message().contains("邮箱"));
    }

    @Test
    @DisplayName("不同邮箱 - 互不影响")
    void testDifferentEmails_Independent() {
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        RegistrationRateLimiter.RateLimitResult result1 =
            RegistrationRateLimiter.checkEmailLimit(email1);
        RegistrationRateLimiter.RateLimitResult result2 =
            RegistrationRateLimiter.checkEmailLimit(email2);
        assertTrue(result1.allowed());
        assertTrue(result2.allowed());
    }

    @Test
    @DisplayName("空邮箱 - 处理")
    void testEmptyEmail() {
        RegistrationRateLimiter.RateLimitResult result =
            RegistrationRateLimiter.checkEmailLimit("");
        assertNotNull(result);
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("null 邮箱 - 处理")
    void testNullEmail() {
        RegistrationRateLimiter.RateLimitResult result =
            RegistrationRateLimiter.checkEmailLimit(null);
        assertNotNull(result);
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("RateLimitResult - 允许状态")
    void testRateLimitResult_Allowed() {
        RegistrationRateLimiter.RateLimitResult result =
            new RegistrationRateLimiter.RateLimitResult(true, "");
        assertTrue(result.allowed());
        assertEquals("", result.message());
    }

    @Test
    @DisplayName("RateLimitResult - 拒绝状态")
    void testRateLimitResult_Blocked() {
        RegistrationRateLimiter.RateLimitResult result =
            new RegistrationRateLimiter.RateLimitResult(false, "注册过于频繁");
        assertFalse(result.allowed());
        assertEquals("注册过于频繁", result.message());
    }

    @Test
    @DisplayName("大量不同邮箱 - 性能测试")
    void testManyDifferentEmails_Performance() {
        for (int i = 0; i < 100; i++) {
            String email = "user" + i + "@test.com";
            RegistrationRateLimiter.RateLimitResult result =
                RegistrationRateLimiter.checkEmailLimit(email);
            assertTrue(result.allowed());
        }
    }
}
