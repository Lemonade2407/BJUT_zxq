package com.bjutzxq.server.config;

import com.bjutzxq.server.util.RegistrationRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务配置
 */
@Slf4j
@Component
public class ScheduledTasks {
    
    /**
     * 每小时清理一次过期的注册记录
     */
    // TODO: 添加更多定时任务：日志清理、统计数据更新、过期数据清理等
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void cleanupRegistrationRecords() {
        log.info("开始清理过期的注册记录...");
        RegistrationRateLimiter.cleanupExpiredRecords();
        log.info("过期注册记录清理完成");
    }
}
