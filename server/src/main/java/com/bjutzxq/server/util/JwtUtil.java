package com.bjutzxq.server.util;

import com.bjutzxq.common.Constants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类
 * <p>
 * 双 Token 模型：
 * - Access Token：15 分钟过期，JWT 格式，包含 userId(jti) 和 tokenVersion(ver)
 * - Refresh Token：7 天过期，不透明字符串，由 RefreshTokenStore 管理
 */
@Slf4j
@Component
public class JwtUtil {

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
        Constants.JWT.TOKEN_SECRET.getBytes(StandardCharsets.UTF_8)
    );

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private TokenVersionManager tokenVersionManager;

    private static JwtUtil instance;
    private static RefreshTokenStore refreshStore;
    private static TokenVersionManager versionManager;

    @PostConstruct
    void init() {
        instance = this;
        refreshStore = this.refreshTokenStore;
        versionManager = this.tokenVersionManager;
    }

    // ==================== 内部类 ====================

    /**
     * Token 对（Access Token + Refresh Token）
     */
    @Data
    @AllArgsConstructor
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
        private long expiresIn; // Access Token 过期时间（秒）
    }

    // ==================== Access Token 生成 ====================

    /**
     * 生成 Access Token（使用当前 token 版本号）
     */
    public static String generateAccessToken(Integer userId) {
        long ver = versionManager.getCurrentVersion(userId);
        return generateToken(userId, ver);
    }

    /**
     * 生成包含指定版本号的 Access Token
     */
    private static String generateToken(Integer userId, long tokenVersion) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + Constants.JWT.ACCESS_TOKEN_EXPIRE);

        String token = Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("ver", tokenVersion)
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expireDate)
            .signWith(SECRET_KEY)
            .compact();

        log.debug("Access Token 生成成功，用户 ID: {}, 版本号: {}, 过期时间: {}",
            userId, tokenVersion, expireDate);
        return token;
    }

    /**
     * 生成完整的 Token 对（Access + Refresh）
     */
    public static TokenPair generateTokenPair(Integer userId) {
        long ver = versionManager.getCurrentVersion(userId);
        String accessToken = generateToken(userId, ver);
        String refreshToken = refreshStore.createRefreshToken(userId, ver);
        return new TokenPair(accessToken, refreshToken,
            Constants.JWT.ACCESS_TOKEN_EXPIRE / 1000);
    }

    // ==================== Token 刷新 ====================

    /**
     * 使用 Refresh Token 刷新 Access Token
     * @param rawRefreshToken 原始 refresh token 字符串
     * @return 新的 Token 对
     */
    public static TokenPair refreshAccessToken(String rawRefreshToken) {
        RefreshTokenStore.RotateResult result = refreshStore.validateAndRotate(rawRefreshToken);
        long ver = versionManager.getCurrentVersion(result.getUserId());
        String newAccessToken = generateToken(result.getUserId(), ver);
        log.info("Access Token 刷新成功，用户 ID: {}", result.getUserId());
        return new TokenPair(newAccessToken, result.getNewRefreshToken(),
            Constants.JWT.ACCESS_TOKEN_EXPIRE / 1000);
    }

    // ==================== Token 验证与解析 ====================

    /**
     * 验证 Token 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.debug("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public static Integer getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return Integer.parseInt(claims.getSubject());
        } catch (Exception e) {
            log.error("从 Token 中获取用户 ID 失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 检查 Token 是否即将过期（剩余时间少于 5 分钟）
     */
    public static boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseToken(token);
            long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
            boolean expiringSoon = remainingTime < 300_000; // 5 分钟
            log.debug("Token 剩余时间: {} 分钟, 即将过期: {}", remainingTime / 60000, expiringSoon);
            return expiringSoon;
        } catch (Exception e) {
            log.debug("检查 Token 过期状态失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Token 剩余有效时间（秒）
     */
    public static long getTokenRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            return (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
        } catch (Exception e) {
            log.debug("获取 Token 剩余时间失败: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * 解析 Token
     */
    private static Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(SECRET_KEY)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
