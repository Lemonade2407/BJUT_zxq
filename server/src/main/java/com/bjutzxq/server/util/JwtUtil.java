package com.bjutzxq.server.util;

import com.bjutzxq.common.Constants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    /**
     * 密钥
     * SecretKey：用于 HMAC-SHA256 签名算法
     * Keys.hmacShaKeyFor()：将字节数组转换为 HMAC 密钥对象
     * .getBytes(StandardCharsets.UTF_8)：将字符串转为 UTF-8 编码的字节数组
     */
    // TODO: 密钥应从环境变量或配置中心读取，不应硬编码在代码中
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
        Constants.JWT.TOKEN_SECRET.getBytes(StandardCharsets.UTF_8)
    );
    
    /**
     * 生成 Token
     * @param userId 用户 ID
     * @param username 用户名
     * @param avatar 头像 URL
     * @return Token 字符串
     */
    public static String generateToken(Integer userId, String username,String avatar) {
        logger.debug("生成 Token，用户 ID: {}, 用户名：{}", userId, username);
        
        //Token 签发时间
        Date now = new Date();
        //Token 过期时间
        Date expireDate = new Date(now.getTime() + Constants.JWT.TOKEN_EXPIRE_TIME);
        logger.debug("Token 过期时间：{}", expireDate);
        
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
        
        logger.debug("Token 生成成功");
        return token;
    }
    
    /**
     * 刷新 Token（延长有效期）
     * @param oldToken 旧 Token
     * @return 新 Token
     */
    public static String refreshToken(String oldToken) {
        logger.debug("刷新 Token");
        
        // TODO: 添加 Token 黑名单机制，防止旧 Token 被重用
        // TODO: 限制 Token 刷新次数，防止无限续期
        
        try {
            Claims claims = parseToken(oldToken);
            Integer userId = Integer.parseInt(claims.getSubject());
            String username = claims.get("username", String.class);
            String avatar = claims.get("avatar", String.class);
            
            // 生成新的 Token
            String newToken = generateToken(userId, username, avatar);
            logger.info("Token 刷新成功，用户 ID: {}", userId);
            return newToken;
        } catch (Exception e) {
            logger.error("Token 刷新失败：{}", e.getMessage());
            throw e;
        }
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
            logger.debug("Token 剩余时间: {} 分钟，是否即将过期: {}", 
                remainingTime / 60000, expiringSoon);
            return expiringSoon;
        } catch (Exception e) {
            logger.error("检查 Token 过期状态失败：{}", e.getMessage());
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
            logger.error("获取 Token 剩余时间失败：{}", e.getMessage());
            return -1;
        }
    }
    
    /**
     * 从 Token 中获取用户 ID
     * @param token Token 字符串
     * @return 用户 ID
     */
    public static Integer getUserIdFromToken(String token) {
        logger.debug("从 Token 中获取用户 ID");
        try {
            Claims claims = parseToken(token);
            Integer userId = Integer.parseInt(claims.getSubject());
            logger.debug("获取用户 ID 成功：{}", userId);
            return userId;
        } catch (Exception e) {
            logger.error("从 Token 中获取用户 ID 失败：{}", e.getMessage());
            throw e;
        }
    }
    /**
     * 从 Token 中获取用户名
     * @param token Token 字符串
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        logger.debug("从 Token 中获取用户名");
        try {
            Claims claims = parseToken(token);
            String username = claims.get("username", String.class);
            logger.debug("获取用户名成功：{}", username);
            return username;
        } catch (Exception e) {
            logger.error("从 Token 中获取用户名失败：{}", e.getMessage());
            throw e;
        }
    }
    /**
     * 从 Token 中获取头像URL
     * @param token Token 字符串
     * @return 头像URL
     */
    public static String getAvatarFromToken(String token) {
        logger.debug("从 Token 中获取头像 URL");
        try {
            Claims claims = parseToken(token);
            String avatar = claims.get("avatar", String.class);
            logger.debug("获取头像 URL 成功：{}", avatar);
            return avatar;
        } catch (Exception e) {
            logger.error("从 Token 中获取头像 URL 失败：{}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 验证 Token 是否有效
     * @param token Token 字符串
     * @return true-有效，false-无效
     */
    public static boolean validateToken(String token) {
        logger.debug("验证 Token 是否有效");
        try {
            parseToken(token);
            logger.debug("Token 验证通过");
            return true;
        } catch (JwtException e) {
            logger.warn("Token 验证失败：{}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 解析 Token
     * @param token Token 字符串
     * @return Claims 对象
     */
    private static Claims parseToken(String token) {
        logger.trace("解析 Token: {}", token.substring(0, Math.min(20, token.length())) + "...");
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
            logger.error("Token 解析失败：{}", e.getMessage());
            throw e;
        }
    }
}
