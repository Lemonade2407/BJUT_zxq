package com.bjutzxq.server.util;

import com.bjutzxq.common.Constants;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Refresh Token 存储管理器
 * 负责 Refresh Token 的 Redis 存储、原子化轮换和吊销
 */
@Slf4j
@Component
public class RefreshTokenStore {

    private static final String REFRESH_PREFIX = "jwt:refresh:";
    private static final String CONSUMED_PREFIX = "jwt:refresh-consumed:";
    private static final String USER_REFRESH_LIST_PREFIX = "jwt:user-refresh-list:";
    private static final long REFRESH_TOKEN_TTL_SECONDS = Constants.JWT.REFRESH_TOKEN_EXPIRE / 1000;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TokenVersionManager tokenVersionManager;

    private DefaultRedisScript<List> refreshRotateScript;

    @Data
    @AllArgsConstructor
    public static class RotateResult {
        private Integer userId;
        private String newRefreshToken;
    }

    @PostConstruct
    void init() {
        refreshRotateScript = new DefaultRedisScript<>();
        refreshRotateScript.setLocation(new ClassPathResource("redis/refresh-rotate.lua"));
        refreshRotateScript.setResultType(List.class);
        log.debug("RefreshToken Lua 脚本已加载");
    }

    /**
     * 创建新的 Refresh Token
     * @param userId 用户 ID
     * @param tokenVersion 当前 token 版本号
     * @return 原始 refresh token 字符串（40 位十六进制）
     */
    public String createRefreshToken(Integer userId, long tokenVersion) {
        String rawToken = generateSecureToken();
        String hash = sha256(rawToken);
        String family = UUID.randomUUID().toString();

        String key = REFRESH_PREFIX + hash;
        redisTemplate.opsForHash().putAll(key, Map.of(
            "userId", String.valueOf(userId),
            "family", family,
            "gen", "1",
            "ver", String.valueOf(tokenVersion)
        ));
        redisTemplate.expire(key, REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        // 加入用户的 refresh token 列表，方便批量吊销
        redisTemplate.opsForSet().add(USER_REFRESH_LIST_PREFIX + userId, hash);
        redisTemplate.expire(USER_REFRESH_LIST_PREFIX + userId, REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        log.debug("已为用户 {} 创建 Refresh Token，家族: {}", userId, family);
        return rawToken;
    }

    /**
     * 原子化验证并轮换 Refresh Token
     * @param rawRefreshToken 原始 refresh token
     * @return 轮换结果（包含用户 ID 和新 token）
     * @throws JwtException 验证失败时抛出
     */
    public RotateResult validateAndRotate(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        String newRawToken = generateSecureToken();
        String newHash = sha256(newRawToken);

        // 1. 读取旧 token 数据
        String oldKey = REFRESH_PREFIX + hash;
        Map<Object, Object> oldData = redisTemplate.opsForHash().entries(oldKey);
        if (oldData.isEmpty()) {
            log.warn("Refresh Token 不存在或已过期");
            throw new JwtException("Refresh Token 无效或已过期");
        }

        Integer userId = Integer.parseInt((String) oldData.get("userId"));
        long oldVer = Long.parseLong((String) oldData.get("ver"));

        // 2. 检查版本号是否匹配
        long currentVer = tokenVersionManager.getCurrentVersion(userId);
        if (oldVer != currentVer) {
            log.warn("Token 版本不匹配: 用户 {} 的 token 版本 {} 不等于当前版本 {}", userId, oldVer, currentVer);
            throw new JwtException("Token 已失效，请重新登录");
        }

        // 3. 执行 Lua 脚本进行原子化轮换
        List<String> keys = Arrays.asList(
            REFRESH_PREFIX + hash,               // KEYS[1] - 旧 token
            CONSUMED_PREFIX + hash,              // KEYS[2] - 已消费标记
            REFRESH_PREFIX + newHash,            // KEYS[3] - 新 token
            USER_REFRESH_LIST_PREFIX + userId    // KEYS[4] - 用户的 token 列表
        );

        String family = (String) oldData.get("family");
        int gen = Integer.parseInt((String) oldData.get("gen"));

        List<Object> result = redisTemplate.execute(
            refreshRotateScript,
            keys,
            family,
            String.valueOf(gen + 1),
            String.valueOf(userId),
            String.valueOf(REFRESH_TOKEN_TTL_SECONDS),
            String.valueOf(currentVer)
        );

        if (result == null || result.isEmpty()) {
            log.error("Lua 脚本执行返回空结果");
            throw new JwtException("Token 刷新失败，请重试");
        }

        long status = ((Number) result.get(0)).longValue();
        if (status == 0) {
            String errorCode = (String) result.get(1);
            log.warn("Token 轮换失败: {}", errorCode);
            switch (errorCode) {
                case "TOKEN_REUSED":
                    throw new JwtException("检测到 Token 重放攻击，已自动保护账号，请重新登录");
                case "TOKEN_NOT_FOUND":
                    throw new JwtException("Refresh Token 无效");
                case "VERSION_MISMATCH":
                    throw new JwtException("Token 已失效，请重新登录");
                default:
                    throw new JwtException("Token 刷新失败");
            }
        }

        log.debug("Refresh Token 轮换成功，用户 ID: {}, 家族: {}, 代数: {}",
            userId, family, gen + 1);
        return new RotateResult(userId, newRawToken);
    }

    /**
     * 删除用户的所有 Refresh Token
     * @param userId 用户 ID
     */
    public void deleteAllForUser(Integer userId) {
        String listKey = USER_REFRESH_LIST_PREFIX + userId;
        Set<String> tokenHashes = redisTemplate.opsForSet().members(listKey);
        if (tokenHashes != null && !tokenHashes.isEmpty()) {
            List<String> tokenKeys = tokenHashes.stream()
                .map(h -> REFRESH_PREFIX + h)
                .collect(Collectors.toList());
            redisTemplate.delete(tokenKeys);
            log.info("已删除用户 {} 的 {} 个 Refresh Token", userId, tokenKeys.size());
        }
        redisTemplate.delete(listKey);
    }

    /**
     * 吊销单个 Refresh Token（登出时使用）
     */
    public void consumeToken(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        String consumedKey = CONSUMED_PREFIX + hash;
        redisTemplate.opsForValue().set(consumedKey, "1", REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.delete(REFRESH_PREFIX + hash);
        log.debug("已吊销 Refresh Token: {}", hash.substring(0, 8));
    }

    // ===== 工具方法 =====

    /**
     * 生成 40 位十六进制安全随机字符串（160 位熵）
     */
    private static String generateSecureToken() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        StringBuilder hex = new StringBuilder(40);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * SHA-256 哈希（返回 64 位小写十六进制）
     */
    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }
}
