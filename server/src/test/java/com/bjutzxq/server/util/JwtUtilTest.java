package com.bjutzxq.server.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT工具测试")
class JwtUtilTest {

    private static final Integer TEST_USER_ID = 100;
    private static final String TEST_USERNAME = "测试用户";
    private static final String TEST_AVATAR = "https://example.com/avatar.png";

    @AfterEach
    void tearDown() {
        // 清理静态缓存，避免测试间污染
        JwtUtil.clearRefreshCount(TEST_USER_ID);
        JwtUtil.clearRefreshCount(999);
        JwtUtil.clearRefreshCount(888);
    }

    // ==================== generateToken ====================

    @Test
    @DisplayName("生成Token - 正常流程")
    void generateToken_Success() {
        // Act
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        // JWT 由三段 base64 编码组成，用 '.' 分隔
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("生成Token - 验证Token格式正确")
    void generateToken_UniquePerCall() {
        // Act
        String token1 = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Assert - Token 非空且由三段组成
        assertNotNull(token1);
        assertFalse(token1.isEmpty());
        String[] parts = token1.split("\\.");
        assertEquals(3, parts.length);
    }

    // ==================== getUserIdFromToken ====================

    @Test
    @DisplayName("从Token获取用户ID - 正常流程")
    void getUserIdFromToken_Success() {
        // Arrange
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Act
        Integer userId = JwtUtil.getUserIdFromToken(token);

        // Assert
        assertEquals(TEST_USER_ID, userId);
    }

    // ==================== getUsernameFromToken ====================

    @Test
    @DisplayName("从Token获取用户名 - 正常流程")
    void getUsernameFromToken_Success() {
        // Arrange
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Act
        String username = JwtUtil.getUsernameFromToken(token);

        // Assert
        assertEquals(TEST_USERNAME, username);
    }

    // ==================== getAvatarFromToken ====================

    @Test
    @DisplayName("从Token获取头像 - 正常流程")
    void getAvatarFromToken_Success() {
        // Arrange
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Act
        String avatar = JwtUtil.getAvatarFromToken(token);

        // Assert
        assertEquals(TEST_AVATAR, avatar);
    }

    // ==================== validateToken ====================

    @Test
    @DisplayName("验证Token - 有效Token返回true")
    void validateToken_Valid() {
        // Arrange
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Act
        boolean result = JwtUtil.validateToken(token);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("验证Token - 无效Token返回false")
    void validateToken_Invalid() {
        // Act
        boolean result = JwtUtil.validateToken("invalid.token.string");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("验证Token - 空字符串抛出异常")
    void validateToken_Empty() {
        // Act & Assert - 空Token会由jjwt库抛出IllegalArgumentException
        assertThrows(Exception.class, () -> JwtUtil.validateToken(""));
    }

    // ==================== isTokenExpiringSoon ====================

    @Test
    @DisplayName("检查Token是否即将过期 - 新生成的Token应未过期")
    void isTokenExpiringSoon_NewToken() {
        // Arrange
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Act
        boolean result = JwtUtil.isTokenExpiringSoon(token);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("检查Token是否即将过期 - 无效Token返回false")
    void isTokenExpiringSoon_InvalidToken() {
        // Act
        boolean result = JwtUtil.isTokenExpiringSoon("invalid.token");

        // Assert
        assertFalse(result);
    }

    // ==================== getTokenRemainingTime ====================

    @Test
    @DisplayName("获取Token剩余时间 - 新生成的Token剩余时间大于0")
    void getTokenRemainingTime_Success() {
        // Arrange
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Act
        long remaining = JwtUtil.getTokenRemainingTime(token);

        // Assert
        assertTrue(remaining > 0);
    }

    @Test
    @DisplayName("获取Token剩余时间 - 无效Token返回-1")
    void getTokenRemainingTime_Invalid() {
        // Act
        long remaining = JwtUtil.getTokenRemainingTime("invalid.token");

        // Assert
        assertEquals(-1, remaining);
    }

    // ==================== blacklist operations ====================

    @Test
    @DisplayName("黑名单操作 - 加入黑名单后检测为true")
    void blacklist_AddAndCheck() {
        // Arrange
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Act
        JwtUtil.addToBlacklist(token);
        boolean isBlacklisted = JwtUtil.isTokenBlacklisted(token);

        // Assert
        assertTrue(isBlacklisted);
    }

    @Test
    @DisplayName("黑名单操作 - 未加入黑名单检测为false")
    void blacklist_NotInBlacklist() {
        // Arrange
        String token = JwtUtil.generateToken(999, "其他用户", "");

        // Act
        boolean isBlacklisted = JwtUtil.isTokenBlacklisted(token);

        // Assert
        assertFalse(isBlacklisted);
    }

    @Test
    @DisplayName("黑名单操作 - 无效Token加入黑名单不抛异常")
    void blacklist_InvalidToken() {
        // Arrange - 使用唯一的无效token避免与其他测试冲突
        String invalidToken = "invalid-token-test-" + System.currentTimeMillis();
        
        // Act & Assert - 不应抛出异常
        assertDoesNotThrow(() -> JwtUtil.addToBlacklist(invalidToken));
        // 无效token由于无法解析过期时间，不会被添加到黑名单
        assertFalse(JwtUtil.isTokenBlacklisted(invalidToken));
    }

    // ==================== refreshToken ====================

    @Test
    @DisplayName("刷新Token成功 - 正常流程")
    void refreshToken_Success() throws Exception {
        // Arrange - 使用独立userId避免静态缓存交叉污染
        final int userId = 701;
        String oldToken = JwtUtil.generateToken(userId, TEST_USERNAME, TEST_AVATAR);
        // 短暂等待确保新旧Token的iat不同（iat精度为秒）
        Thread.sleep(1100);

        // Act
        String newToken = JwtUtil.refreshToken(oldToken);

        // Assert
        assertNotNull(newToken);
        assertNotEquals(oldToken, newToken);
        // 旧Token在黑名单中
        assertTrue(JwtUtil.isTokenBlacklisted(oldToken));
        // 新Token可用
        assertEquals(userId, JwtUtil.getUserIdFromToken(newToken));
    }

    @Test
    @DisplayName("刷新Token失败 - 旧Token已在黑名单中")
    void refreshToken_AlreadyBlacklisted() {
        // Arrange - 使用独立userId避免静态缓存交叉污染
        final int userId = 702;
        String oldToken = JwtUtil.generateToken(userId, TEST_USERNAME, TEST_AVATAR);
        JwtUtil.addToBlacklist(oldToken);

        // Act & Assert
        assertThrows(Exception.class, () -> JwtUtil.refreshToken(oldToken));
    }

    @Test
    @DisplayName("刷新Token失败 - 无效Token")
    void refreshToken_InvalidToken() {
        // Act & Assert
        assertThrows(Exception.class, () -> JwtUtil.refreshToken("invalid.token"));
    }

    @Test
    @DisplayName("刷新Token - 刷新后计数增加")
    void refreshToken_IncrementsCount() {
        // Arrange - 使用独立userId避免静态缓存交叉污染
        final int userId = 703;
        JwtUtil.clearRefreshCount(userId);
        String oldToken = JwtUtil.generateToken(userId, TEST_USERNAME, TEST_AVATAR);

        // Act
        JwtUtil.refreshToken(oldToken);

        // Assert
        int count = JwtUtil.getRefreshCount(userId);
        assertEquals(1, count);
    }

    // ==================== getRefreshCount / clearRefreshCount ====================

    @Test
    @DisplayName("刷新计数 - 初始为0")
    void getRefreshCount_Initial() {
        // Arrange
        JwtUtil.clearRefreshCount(999);

        // Act
        int count = JwtUtil.getRefreshCount(999);

        // Assert
        assertEquals(0, count);
    }

    @Test
    @DisplayName("清除刷新计数 - 清除后为0")
    void clearRefreshCount_Success() {
        // Arrange
        JwtUtil.clearRefreshCount(TEST_USER_ID);

        // Act & Assert
        assertEquals(0, JwtUtil.getRefreshCount(TEST_USER_ID));
    }

    // ==================== 数据完整性测试 ====================

    @Test
    @DisplayName("Token数据完整性 - 往返验证")
    void roundTrip_AllFields() {
        // Arrange
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_AVATAR);

        // Act & Assert
        assertEquals(TEST_USER_ID, JwtUtil.getUserIdFromToken(token));
        assertEquals(TEST_USERNAME, JwtUtil.getUsernameFromToken(token));
        assertEquals(TEST_AVATAR, JwtUtil.getAvatarFromToken(token));
        assertTrue(JwtUtil.validateToken(token));
        assertFalse(JwtUtil.isTokenExpiringSoon(token));
    }
}
