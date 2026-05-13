package com.bjutzxq.server.config;

import com.bjutzxq.server.handler.NotificationWebSocketHandler;
import com.bjutzxq.server.util.RegistrationRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 定时任务配置
 */
@Slf4j
@Component
public class ScheduledTasks {
    
    @Autowired
    private com.bjutzxq.server.mapper.NotificationMapper notificationMapper;
    
    @Autowired
    private NotificationWebSocketHandler webSocketHandler;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Autowired
    private com.bjutzxq.server.mapper.ProjectMapper projectMapper;
    
    /**
     * 每天凌晨3点清理过期的注册记录
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void cleanupRegistrationRecords() {
        log.info("开始清理过期的注册记录...");
        RegistrationRateLimiter.cleanupExpiredRecords();
        log.info("过期注册记录清理完成");
    }
    
    /**
     * 每天凌晨2点清理旧通知记录
     * - 已读通知：保留30天
     * - 未读通知：不删除（让用户自行管理）
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupOldNotifications() {
        log.info("开始清理旧通知记录...");
        try {
            // 计算30天前的日期
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            // 只删除已读且超过30天的通知
            int deletedCount = notificationMapper.deleteReadBeforeDate(thirtyDaysAgo);
            log.info("通知记录清理完成，删除了 {} 条已读通知", deletedCount);
        } catch (Exception e) {
            log.error("清理通知记录失败", e);
        }
    }
    
    /**
     * 每5分钟将 Redis 中的项目查看计数批量刷新到 MySQL
     */
    @Scheduled(fixedRate = 300000)
    public void flushViewCounts() {
        var keys = redisTemplate.keys("pv:project:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                String countStr = redisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    int count = Integer.parseInt(countStr);
                    if (count > 0) {
                        int projectId = Integer.parseInt(key.substring(key.lastIndexOf(':') + 1));
                        projectMapper.incrementViewCountBy(projectId, count);
                        redisTemplate.opsForValue().decrement(key, count);
                    }
                }
            } catch (Exception e) {
                log.warn("刷新查看计数失败: key={}, error={}", key, e.getMessage());
            }
        }
    }

    /**
     * 每5分钟清理超时的 WebSocket 连接
     * 检测超过60秒未发送心跳的连接并关闭
     */
    @Scheduled(fixedRate = 300000) // 5分钟执行一次
    public void cleanupTimeoutWebSocketConnections() {
        log.debug("开始检查超时的 WebSocket 连接...");
        
        int cleanedCount = 0;
        
        // 遍历所有在线用户，检查是否超时
        for (Integer userId : webSocketHandler.getOnlineUserIds()) {
            if (webSocketHandler.isUserTimeout(userId)) {
                log.warn("用户 {} 的 WebSocket 连接超时（超过60秒无心跳），准备关闭", userId);
                
                WebSocketSession session = webSocketHandler.getUserSession(userId);
                if (session != null && session.isOpen()) {
                    try {
                        session.close(CloseStatus.GOING_AWAY.withReason("连接超时"));
                        cleanedCount++;
                        log.info("已关闭用户 {} 的超时 WebSocket 连接", userId);
                    } catch (IOException e) {
                        log.error("关闭用户 {} 的超时连接失败", userId, e);
                    }
                }
            }
        }
        
        if (cleanedCount > 0) {
            log.info("WebSocket 超时连接清理完成，共清理 {} 个连接，当前在线用户数: {}", 
                    cleanedCount, webSocketHandler.getOnlineUserCount());
        } else {
            log.debug("未发现超时的 WebSocket 连接，当前在线用户数: {}", 
                    webSocketHandler.getOnlineUserCount());
        }
    }
}
