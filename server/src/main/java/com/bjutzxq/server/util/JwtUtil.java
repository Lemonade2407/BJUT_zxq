package com.bjutzxq.server.util;

import com.bjutzxq.common.Constants;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JWT 工具类
 */
@Slf4j
public class JwtUtil {
    
    /**
     * 密钥
     * SecretKey：用于 HMAC-SHA256 签名算法
     * Keys.hmacShaKeyFor()：将字节数组转换为 HMAC 密钥对象
     * .getBytes(StandardCharsets.UTF_8)：将字符串转为 UTF-8 编码的字节数组
     */
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
        Constants.JWT.TOKEN_SECRET.getBytes(StandardCharsets.UTF_8)
    );
    
    /**
     * Token 黑名单缓存（key: token, value: 过期时间）
     * 用于防止旧 Token 被重用
     * 缓存过期时间设置为 Token 有效期的 2 倍，确保完全覆盖
     */
    private static final Cache<String, Long> TOKEN_BLACKLIST = Caffeine.newBuilder()
        .maximumSize(10000) // 最大存储 10000 个黑名单 Token
        .expireAfterWrite(Duration.ofMillis(Constants.JWT.TOKEN_EXPIRE_TIME * 2))
        .recordStats() // 记录统计信息
        .build();
    
    /**
     * Token 刷新次数计数器（key: userId, value: 刷新次数）
     * 用于限制单个用户的 Token 刷新次数，防止无限续期
     * 缓存过期时间为 1 小时，每小时重置计数
     */
    private static final Cache<Integer, AtomicInteger> REFRESH_COUNTER = Caffeine.newBuilder()
        .maximumSize(5000) // 最大存储 5000 个用户
        .expireAfterWrite(Duration.ofHours(1))
        .recordStats()
        .build();
    
    /**
     * 最大刷新次数：每个用户每小时最多刷新 10 次
     */
    private static final int MAX_REFRESH_COUNT = 10;
    
    /**
     * 生成 Token
     * @param userId 用户 ID
     * @param username 用户名
     * @param avatar 头像 URL
     * @return Token 字符串
     */
    public static String generateToken(Integer userId, String username,String avatar) {
        log.debug("生成 Token，用户 ID: {}, 用户名：{}", userId, username);
        
        //Token 签发时间
        Date now = new Date();
        //Token 过期时间
        Date expireDate = new Date(now.getTime() + Constants.JWT.TOKEN_EXPIRE_TIME);
        log.debug("Token 过期时间：{}", expireDate);
        
        //构建 Token
        String token = Jwts.builder()
            //设置 Token 的 Subject（用户 ID）
            .subject(String.valueOf(userId))
            //设置 Token 的 Payload（用户名和头像）
            .claim("username", username)
            .claim("avatar",avatar)
            //设置 Token 的签发时间
            .issuedAt(now)
            //设置 Token 的过期时间
            .expiration(expireDate)
            //使用 HMAC-SHA256 算法和密钥进行签名
            .signWith(SECRET_KEY)
            //生成紧凑格式的 JWT 字符串
            .compact();
        
        log.debug("Token 生成成功");
        return token;
    }
    
    /**
     * 刷新 Token（延长有效期）
     * @param oldToken 旧 Token
     * @return 新 Token
     */
    public static String refreshToken(String oldToken) {
        log.debug("刷新 Token");
        
        try {
            // 1. 检查 Token 是否在黑名单中（防止重用）
            if (isTokenBlacklisted(oldToken)) {
                log.warn("Token 刷新失败：该 Token 已被加入黑名单，可能存在重放攻击");
                throw new JwtException("Token 已失效，请重新登录");
            }
            
            // 2. 解析旧 Token 获取用户信息
            Claims claims = parseToken(oldToken);
            Integer userId = Integer.parseInt(claims.getSubject());
            String username = claims.get("username", String.class);
            String avatar = claims.get("avatar", String.class);
            
            // 3. 检查刷新次数限制（防止无限续期）
            if (!checkRefreshLimit(userId)) {
                log.warn("Token 刷新失败：用户 {} 超过最大刷新次数限制", userId);
                throw new JwtException("Token 刷新次数过多，请重新登录");
            }
            
            // 4. 将旧 Token 加入黑名单
            addToBlacklist(oldToken);
            
            // 5. 增加刷新计数
            incrementRefreshCount(userId);
            
            // 6. 生成新的 Token
            String newToken = generateToken(userId, username, avatar);
            log.info("Token 刷新成功，用户 ID: {}, 当前刷新次数: {}", 
                userId, getRefreshCount(userId));
            return newToken;
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token 刷新失败：{}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 检查 Token 是否在黑名单中
     * @param token Token 字符串
     * @return true-在黑名单中，false-不在黑名单中
     */
    public static boolean isTokenBlacklisted(String token) {
        return TOKEN_BLACKLIST.getIfPresent(token) != null;
    }
    
    /**
     * 将 Token 加入黑名单
     * @param token Token 字符串
     */
    public static void addToBlacklist(String token) {
        try {
            Claims claims = parseToken(token);
            long expireTime = claims.getExpiration().getTime();
            TOKEN_BLACKLIST.put(token, expireTime);
            log.debug("Token 已加入黑名单");
        } catch (Exception e) {
            log.warn("加入黑名单失败：{}", e.getMessage());
        }
    }
    
    /**
     * 检查用户是否超过刷新次数限制
     * @param userId 用户 ID
     * @return true-未超过限制，false-超过限制
     */
    private static boolean checkRefreshLimit(Integer userId) {
        AtomicInteger counter = REFRESH_COUNTER.getIfPresent(userId);
        if (counter == null) {
            return true; // 首次刷新，允许
        }
        return counter.get() < MAX_REFRESH_COUNT;
    }
    
    /**
     * 增加用户的刷新计数
     * @param userId 用户 ID
     */
    private static void incrementRefreshCount(Integer userId) {
        // 使用 get 方法，如果不存在则创建新的计数器
        AtomicInteger counter = REFRESH_COUNTER.get(userId, k -> new AtomicInteger(0));
        counter.incrementAndGet();
    }
    
    /**
     * 获取用户的当前刷新次数
     * @param userId 用户 ID
     * @return 刷新次数
     */
    public static int getRefreshCount(Integer userId) {
        AtomicInteger counter = REFRESH_COUNTER.getIfPresent(userId);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * 清除用户的刷新计数（例如用户重新登录后）
     * @param userId 用户 ID
     */
    public static void clearRefreshCount(Integer userId) {
        REFRESH_COUNTER.invalidate(userId);
        log.debug("已清除用户 {} 的刷新计数", userId);
    }
    
    /**
     * 检查 Token 是否即将过期（剩余时间少于 30 分钟）
     * @param token Token 字符串
     * @return true-即将过期，false-未即将过期
     */
    public static boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            
            // 剩余时间少于 30 分钟（1800000 毫秒）
            boolean expiringSoon = remainingTime < 1800000;
            log.debug("Token 剩余时间: {} 分钟，是否即将过期: {}",
                remainingTime / 60000, expiringSoon);
            return expiringSoon;
        } catch (Exception e) {
            log.error("检查 Token 过期状态失败：{}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取 Token 剩余有效时间（秒）
     * @param token Token 字符串
     * @return 剩余秒数，如果已过期返回负数
     */
    public static long getTokenRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            return remainingTime / 1000; // 转换为秒
        } catch (Exception e) {
            log.error("获取 Token 剩余时间失败：{}", e.getMessage());
            return -1;
        }
    }
    
    /**
     * 从 Token 中获取用户 ID
     * @param token Token 字符串
     * @return 用户 ID
     */
    public static Integer getUserIdFromToken(String token) {
        log.debug("从 Token 中获取用户 ID");
        try {
            Claims claims = parseToken(token);
            Integer userId = Integer.parseInt(claims.getSubject());
            log.debug("获取用户 ID 成功：{}", userId);
            return userId;
        } catch (Exception e) {
            log.error("从 Token 中获取用户 ID 失败：{}", e.getMessage());
            throw e;
        }
    }
    /**
     * 从 Token 中获取用户名
     * @param token Token 字符串
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        log.debug("从 Token 中获取用户名");
        try {
            Claims claims = parseToken(token);
            String username = claims.get("username", String.class);
            log.debug("获取用户名成功：{}", username);
            return username;
        } catch (Exception e) {
            log.error("从 Token 中获取用户名失败：{}", e.getMessage());
            throw e;
        }
    }
    /**
     * 从 Token 中获取头像URL
     * @param token Token 字符串
     * @return 头像URL
     */
    public static String getAvatarFromToken(String token) {
        log.debug("从 Token 中获取头像 URL");
        try {
            Claims claims = parseToken(token);
            String avatar = claims.get("avatar", String.class);
            log.debug("获取头像 URL 成功：{}", avatar);
            return avatar;
        } catch (Exception e) {
            log.error("从 Token 中获取头像 URL 失败：{}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 验证 Token 是否有效
     * @param token Token 字符串
     * @return true-有效，false-无效
     */
    public static boolean validateToken(String token) {
        log.debug("验证 Token 是否有效");
        try {
            parseToken(token);
            log.debug("Token 验证通过");
            return true;
        } catch (JwtException e) {
            log.warn("Token 验证失败：{}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 解析 Token
     * @param token Token 字符串
     * @return Claims 对象
     */
    private static Claims parseToken(String token) {
        log.trace("解析 Token: {}", token.substring(0, Math.min(20, token.length())) + "...");
        try {
            return Jwts
                    // 创建解析器
                    .parser()
                    // 设置密钥
                    .verifyWith(SECRET_KEY)
                    // 创建解析器
                    .build()
                    // 解析并验证 Token
                    .parseSignedClaims(token)
                    // 获取 payload 数据
                    .getPayload();
        } catch (JwtException e) {
            log.error("Token 解析失败：{}", e.getMessage());
            throw e;
        }
    }
}
