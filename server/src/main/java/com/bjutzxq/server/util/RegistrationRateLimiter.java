package com.bjutzxq.server.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RegistrationRateLimiter {

    private static final int MAX_PER_EMAIL_PER_DAY = 3;
    private static final long DAY_SECONDS = 24 * 60 * 60;
    private static final String REDIS_PREFIX = "reg:email:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static RegistrationRateLimiter instance;
    private static final java.util.Map<String, Long> FALLBACK = new java.util.concurrent.ConcurrentHashMap<>();

    @PostConstruct
    void init() { instance = this; }

    private boolean redisAvailable() {
        return instance != null && instance.redisTemplate != null;
    }

    public static RateLimitResult checkEmailLimit(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new RateLimitResult(false, "邮箱地址无效");
        }

        String key = REDIS_PREFIX + email.trim().toLowerCase();

        if (instance != null && instance.redisAvailable()) {
            Long count = instance.redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                instance.redisTemplate.expire(key, DAY_SECONDS, TimeUnit.SECONDS);
            }
            if (count != null && count > MAX_PER_EMAIL_PER_DAY) {
                Long ttl = instance.redisTemplate.getExpire(key, TimeUnit.HOURS);
                long hours = (ttl != null && ttl > 0) ? ttl : 24;
                log.warn("邮箱注册频率超限：{}, 剩余: {} 小时", email, hours);
                return new RateLimitResult(false,
                    String.format("该邮箱注册过于频繁，请 %d 小时后再试", hours + 1));
            }
        } else {
            // 降级：用内存 Map
            long now = System.currentTimeMillis();
            Long count = FALLBACK.getOrDefault(key, 0L) + 1;
            FALLBACK.put(key, count);
            if (count > MAX_PER_EMAIL_PER_DAY) {
                return new RateLimitResult(false, "该邮箱注册过于频繁，请稍后再试");
            }
        }

        return new RateLimitResult(true, "");
    }

    public static void cleanupExpiredRecords() {
        // Redis 自动过期，无需手动清理
    }

    public static void clearAllRecords() {
        if (instance != null && instance.redisAvailable()) {
            Set<String> keys = instance.redisTemplate.keys(REDIS_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                instance.redisTemplate.delete(keys);
            }
        } else {
            FALLBACK.clear();
        }
        log.debug("清空所有注册记录");
    }

    public record RateLimitResult(boolean allowed, String message) {}
}
