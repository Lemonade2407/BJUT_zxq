package com.bjutzxq.server.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册频率限制器
 */
@Slf4j
public class RegistrationRateLimiter {
    
    /**
     * 邮箱注册记录存储（生产环境应使用 Redis）
     */
    // TODO: 当前使用内存存储，重启后数据丢失，生产环境应迁移到 Redis
    private static final Map<String, RegistrationRecord> EMAIL_REGISTRATION_RECORDS = new ConcurrentHashMap<>();

    /**
     * 同一邮箱最大注册次数（每天）
     */
    private static final int MAX_REGISTRATIONS_PER_EMAIL_PER_DAY = 3;

    /**
     * 时间窗口（毫秒）
     */
    private static final long EMAIL_TIME_WINDOW = 24 * 60 * 60 * 1000; // 24小时
    
    /**
     * 检查邮箱是否超过注册频率限制
     * @param email 邮箱地址
     * @return 是否允许注册
     */
    public static RateLimitResult checkEmailLimit(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("邮箱地址为空");
            return new RateLimitResult(false, "邮箱地址无效");
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        RegistrationRecord record = EMAIL_REGISTRATION_RECORDS.get(normalizedEmail);
        long currentTime = System.currentTimeMillis();
        
        // 如果没有记录或记录已过期，创建新记录
        if (record == null || (currentTime - record.getFirstAttemptTime()) > EMAIL_TIME_WINDOW) {
            EMAIL_REGISTRATION_RECORDS.put(normalizedEmail, new RegistrationRecord(1, currentTime));
            log.debug("邮箱注册记录初始化：{}", normalizedEmail);
            return new RateLimitResult(true, "");
        }
        
        // 检查是否在时间窗口内
        if ((currentTime - record.getFirstAttemptTime()) <= EMAIL_TIME_WINDOW) {
            // 检查是否超过限制
            if (record.getCount() >= MAX_REGISTRATIONS_PER_EMAIL_PER_DAY) {
                long remainingTime = EMAIL_TIME_WINDOW - (currentTime - record.getFirstAttemptTime());
                long hours = remainingTime / (60 * 60 * 1000);
                log.warn("邮箱注册频率超限：{}, 剩余时间: {} 小时", normalizedEmail, hours);
                return new RateLimitResult(false, 
                    String.format("该邮箱注册过于频繁，请 %d 小时后再试", hours + 1));
            }
            
            // 增加计数
            record.increment();
            log.debug("邮箱注册计数：{}, 次数：{}", normalizedEmail, record.getCount());
        } else {
            // 时间窗口已过，重置计数
            EMAIL_REGISTRATION_RECORDS.put(normalizedEmail, new RegistrationRecord(1, currentTime));
        }
        return new RateLimitResult(true, "");
    }
    
    /**
     * 清理过期的记录（定期调用）
     */
    public static void cleanupExpiredRecords() {
        long currentTime = System.currentTimeMillis();
        
        // 清理邮箱记录
        EMAIL_REGISTRATION_RECORDS.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue().getFirstAttemptTime()) > EMAIL_TIME_WINDOW);
        
        log.debug("清理过期注册记录完成");
    }
    
    /**
     * 清空所有记录（仅用于测试）
     */
    public static void clearAllRecords() {
        EMAIL_REGISTRATION_RECORDS.clear();
        log.debug("清空所有注册记录");
    }
    
    /**
     * 注册记录内部类
     */
    @Getter
    private static class RegistrationRecord {
        private int count;
        private final long firstAttemptTime;
        
        public RegistrationRecord(int count, long firstAttemptTime) {
            this.count = count;
            this.firstAttemptTime = firstAttemptTime;
        }
        
        public void increment() {
            this.count++;
        }

    }
    
    /**
     * 频率限制结果
     */
    public record RateLimitResult(boolean allowed, String message) {}
}
