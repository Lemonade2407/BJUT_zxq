package com.bjutzxq.server.util;

import com.bjutzxq.common.Constants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JWT工具测试")
class JwtUtilTest {

    private static final Integer TEST_USER_ID = 100;
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
        Constants.JWT.TOKEN_SECRET.getBytes(StandardCharsets.UTF_8)
    );

    @Mock
    private TokenVersionManager mockVersionManager;

    @Mock
    private RefreshTokenStore mockRefreshStore;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射注入 mock 依赖到 JwtUtil 的静态字段
        setStaticField("versionManager", mockVersionManager);
        setStaticField("refreshStore", mockRefreshStore);

        when(mockVersionManager.getCurrentVersion(anyInt())).thenReturn(1L);
    }

    @AfterEach
    void tearDown() throws Exception {
        // 清理静态字段
        setStaticField("versionManager", null);
        setStaticField("refreshStore", null);
    }

    private void setStaticField(String name, Object value) throws Exception {
        Field field = JwtUtil.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    /**
     * 在测试中手动生成一个带版本号的 token（绕过依赖注入）
     */
    private String createTestToken(Integer userId, long version) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + Constants.JWT.ACCESS_TOKEN_EXPIRE);
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("ver", version)
            .id(java.util.UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expireDate)
            .signWith(SECRET_KEY)
            .compact();
    }

    // ==================== generateAccessToken ====================

    @Test
    @DisplayName("生成Access Token - 正常流程")
    void generateAccessToken_Success() {
        // Act
        String token = JwtUtil.generateAccessToken(TEST_USER_ID);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("生成Access Token - 包含版本号声明")
    void generateAccessToken_ContainsVersion() {
        // Act
        String token = JwtUtil.generateAccessToken(TEST_USER_ID);
        Integer userId = JwtUtil.getUserIdFromToken(token);

        // Assert
        assertEquals(TEST_USER_ID, userId);
    }

    // ==================== generateTokenPair ====================

    @Test
    @DisplayName("生成Token对 - 正常流程")
    void generateTokenPair_Success() {
        // Arrange
        when(mockRefreshStore.createRefreshToken(anyInt(), anyLong()))
            .thenReturn("mock-refresh-token-40-char-hex-string-12345678");

        // Act
        JwtUtil.TokenPair pair = JwtUtil.generateTokenPair(TEST_USER_ID);

        // Assert
        assertNotNull(pair);
        assertNotNull(pair.getAccessToken());
        assertNotNull(pair.getRefreshToken());
        assertTrue(pair.getExpiresIn() > 0);
        assertEquals("mock-refresh-token-40-char-hex-string-12345678", pair.getRefreshToken());
        // 验证 access token 有效
        assertEquals(TEST_USER_ID, JwtUtil.getUserIdFromToken(pair.getAccessToken()));
    }

    // ==================== validateToken ====================

    @Test
    @DisplayName("验证Token - 有效Token返回true")
    void validateToken_Valid() {
        // Arrange
        String token = createTestToken(TEST_USER_ID, 1);

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
        // Act & Assert
        assertThrows(Exception.class, () -> JwtUtil.validateToken(""));
    }

    // ==================== getUserIdFromToken ====================

    @Test
    @DisplayName("从Token获取用户ID - 正常流程")
    void getUserIdFromToken_Success() {
        // Arrange
        String token = createTestToken(TEST_USER_ID, 1);

        // Act
        Integer userId = JwtUtil.getUserIdFromToken(token);

        // Assert
        assertEquals(TEST_USER_ID, userId);
    }

    // ==================== isTokenExpiringSoon ====================

    @Test
    @DisplayName("检查Token是否即将过期 - 新生成的Token应未过期")
    void isTokenExpiringSoon_NewToken() {
        // Arrange
        String token = createTestToken(TEST_USER_ID, 1);

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
        String token = createTestToken(TEST_USER_ID, 1);

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

    // ==================== refreshAccessToken ====================

    @Test
    @DisplayName("刷新Access Token - 正常流程")
    void refreshAccessToken_Success() {
        // Arrange
        when(mockRefreshStore.validateAndRotate(anyString()))
            .thenReturn(new RefreshTokenStore.RotateResult(TEST_USER_ID, "new-refresh-token"));

        // Act
        JwtUtil.TokenPair pair = JwtUtil.refreshAccessToken("old-refresh-token");

        // Assert
        assertNotNull(pair);
        assertNotNull(pair.getAccessToken());
        assertEquals("new-refresh-token", pair.getRefreshToken());
        assertTrue(pair.getExpiresIn() > 0);
        // 新 access token 应包含正确的用户 ID
        assertEquals(TEST_USER_ID, JwtUtil.getUserIdFromToken(pair.getAccessToken()));
    }

    @Test
    @DisplayName("刷新Access Token - 无效Refresh Token应抛异常")
    void refreshAccessToken_InvalidToken() {
        // Arrange
        when(mockRefreshStore.validateAndRotate(anyString()))
            .thenThrow(new io.jsonwebtoken.JwtException("Refresh Token 无效"));

        // Act & Assert
        assertThrows(io.jsonwebtoken.JwtException.class,
            () -> JwtUtil.refreshAccessToken("invalid-refresh-token"));
    }

    // ==================== TokenPair ====================

    @Test
    @DisplayName("TokenPair - 构造和读取")
    void tokenPair_AllFields() {
        // Act
        JwtUtil.TokenPair pair = new JwtUtil.TokenPair("access", "refresh", 900L);

        // Assert
        assertEquals("access", pair.getAccessToken());
        assertEquals("refresh", pair.getRefreshToken());
        assertEquals(900L, pair.getExpiresIn());
    }

    // ==================== 数据完整性测试 ====================

    @Test
    @DisplayName("Token数据完整性 - 往返验证")
    void roundTrip_AllFields() {
        // Arrange
        String token = createTestToken(TEST_USER_ID, 5);

        // Act & Assert
        assertEquals(TEST_USER_ID, JwtUtil.getUserIdFromToken(token));
        assertTrue(JwtUtil.validateToken(token));
        assertFalse(JwtUtil.isTokenExpiringSoon(token));
    }
}
