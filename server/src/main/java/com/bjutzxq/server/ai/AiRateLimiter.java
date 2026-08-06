package com.bjutzxq.server.ai;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

/**
 * AI 对话限流（Redis 计数），控制 DeepSeek token 成本
 */
@Component
public class AiRateLimiter {

    private static final int PER_MINUTE = 10;
    private static final int PER_DAY = 200;

    private final StringRedisTemplate redis;

    public AiRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(Integer userId) {
        String minuteKey = "ai:rl:" + userId + ":" + (System.currentTimeMillis() / 60_000);
        String dayKey = "ai:rl:" + userId + ":" + LocalDate.now();
        Long m = redis.opsForValue().increment(minuteKey);
        if (m != null && m == 1) {
            redis.expire(minuteKey, Duration.ofMinutes(1));
        }
        Long d = redis.opsForValue().increment(dayKey);
        if (d != null && d == 1) {
            redis.expire(dayKey, Duration.ofDays(1));
        }
        if (m != null && m > PER_MINUTE) {
            throw new AiException("提问太频繁，请稍后再试");
        }
        if (d != null && d > PER_DAY) {
            throw new AiException("今日对话次数已达上限，请明天再来");
        }
    }
}
