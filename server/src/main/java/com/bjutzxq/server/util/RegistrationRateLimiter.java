package com.bjutzxq.server.util;

import lombok.Data;
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
     * IP 地址注册记录存储（生产环境应使用 Redis）
     */
    // TODO: 当前使用内存存储，重启后数据丢失，生产环境应迁移到 Redis
    private static final Map<String, RegistrationRecord> IP_REGISTRATION_RECORDS = new ConcurrentHashMap<>();
    
    /**
     * 邮箱注册记录存储
     */
    private static final Map<String, RegistrationRecord> EMAIL_REGISTRATION_RECORDS = new ConcurrentHashMap<>();
    
    /**
     * 同一 IP 最大注册次数（每小时）
     */
    private static final int MAX_REGISTRATIONS_PER_IP_PER_HOUR = 5;
    
    /**
     * 同一邮箱最大注册次数（每天）
     */
    private static final int MAX_REGISTRATIONS_PER_EMAIL_PER_DAY = 3;
    
    /**
     * 时间窗口（毫秒）
     */
    private static final long IP_TIME_WINDOW = 60 * 60 * 1000; // 1小时
    private static final long EMAIL_TIME_WINDOW = 24 * 60 * 60 * 1000; // 24小时
    
    /**
     * 检查 IP 是否超过注册频率限制
     * @param ipAddress IP 地址
     * @return 是否允许注册
     */
    public static RateLimitResult checkIpLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            log.warn("IP 地址为空");
            return new RateLimitResult(false, "IP 地址无效");
        }
        
        String ip = ipAddress.trim();
        RegistrationRecord record = IP_REGISTRATION_RECORDS.get(ip);
        long currentTime = System.currentTimeMillis();
        
        // 如果没有记录或记录已过期，创建新记录
        if (record == null || (currentTime - record.getFirstAttemptTime()) > IP_TIME_WINDOW) {
            IP_REGISTRATION_RECORDS.put(ip, new RegistrationRecord(1, currentTime));
            log.debug("IP 注册记录初始化：{}", ip);
            return new RateLimitResult(true, "");
        }
        
        // 检查是否在时间窗口内
        if ((currentTime - record.getFirstAttemptTime()) <= IP_TIME_WINDOW) {
            // 检查是否超过限制
            if (record.getCount() >= MAX_REGISTRATIONS_PER_IP_PER_HOUR) {
                long remainingTime = IP_TIME_WINDOW - (currentTime - record.getFirstAttemptTime());
                long minutes = remainingTime / (60 * 1000);
                log.warn("IP 注册频率超限：{}, 剩余时间: {} 分钟", ip, minutes);
                return new RateLimitResult(false, 
                    String.format("该 IP 注册过于频繁，请 %d 分钟后再试", minutes + 1));
            }
            
            // 增加计数
            record.increment();
            log.debug("IP 注册计数：{}, 次数：{}", ip, record.getCount());
            return new RateLimitResult(true, "");
        } else {
            // 时间窗口已过，重置计数
            IP_REGISTRATION_RECORDS.put(ip, new RegistrationRecord(1, currentTime));
            return new RateLimitResult(true, "");
        }
    }
    
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
            return new RateLimitResult(true, "");
        } else {
            // 时间窗口已过，重置计数
            EMAIL_REGISTRATION_RECORDS.put(normalizedEmail, new RegistrationRecord(1, currentTime));
            return new RateLimitResult(true, "");
        }
    }
    
    /**
     * 清理过期的记录（定期调用）
     */
    // TODO: 添加定时任务自动清理过期记录，避免内存泄漏
    public static void cleanupExpiredRecords() {
        long currentTime = System.currentTimeMillis();
        
        // 清理 IP 记录
        IP_REGISTRATION_RECORDS.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue().getFirstAttemptTime()) > IP_TIME_WINDOW);
        
        // 清理邮箱记录
        EMAIL_REGISTRATION_RECORDS.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue().getFirstAttemptTime()) > EMAIL_TIME_WINDOW);
        
        log.debug("清理过期注册记录完成");
    }
    
    /**
     * 清空所有记录（仅用于测试）
     */
    public static void clearAllRecords() {
        IP_REGISTRATION_RECORDS.clear();
        EMAIL_REGISTRATION_RECORDS.clear();
        log.debug("清空所有注册记录");
    }
    
    /**
     * 注册记录内部类
     */
    private static class RegistrationRecord {
        private int count;
        private long firstAttemptTime;
        
        public RegistrationRecord(int count, long firstAttemptTime) {
            this.count = count;
            this.firstAttemptTime = firstAttemptTime;
        }
        
        public void increment() {
            this.count++;
        }
        
        public int getCount() {
            return count;
        }
        
        public long getFirstAttemptTime() {
            return firstAttemptTime;
        }
    }
    
    /**
     * 频率限制结果
     */
    @Data
    public static class RateLimitResult {
        private boolean allowed;
        private String message;

        public RateLimitResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
    }
}
