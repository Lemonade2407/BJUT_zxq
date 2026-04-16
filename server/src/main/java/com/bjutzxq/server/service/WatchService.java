package com.bjutzxq.server.service;

import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.Project;
import com.bjutzxq.pojo.Watch;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.server.mapper.WatchMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 关注服务类
 */
@Slf4j
@Service
public class WatchService {
    @Autowired
    private WatchMapper watchMapper;
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 关注项目
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @param notificationType 通知类型：1-全部，2-仅重要更新
     * @return 当前关注数
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer watchProject(Integer userId, Integer projectId, Integer notificationType) {
        log.info("关注项目，用户 ID: {}, 项目 ID: {}, 通知类型：{}", userId, projectId, notificationType);
        
        // 1. 检查是否已经关注
        Watch existing = watchMapper.selectByUserIdAndProjectId(userId, projectId);
        if (existing != null) {
            throw new RuntimeException("您已经关注该项目了");
        }
        
        // 2. 新增关注记录
        Watch watch = new Watch();
        watch.setUserId(userId);
        watch.setProjectId(projectId);
        watch.setNotificationType(notificationType != null ? notificationType : 1);
        watchMapper.insert(watch);
        
        // 3. 原子性增加项目关注数
        projectMapper.incrementWatchCount(projectId);
        
        // 4. 查询最新的关注数
        Project project = projectMapper.selectById(projectId);
        Integer watchCount = project != null && project.getWatchCount() != null ? project.getWatchCount() : 0;
        
        // 5. 创建通知（通知项目所有者）
        try {
            if (project != null && !project.getOwnerId().equals(userId)) {
                String content = "关注了你的项目：" + project.getName();
                notificationService.createNotification(
                    project.getOwnerId(), userId, projectId,
                    NotificationType.WATCH.getCode(), content);
            }
        } catch (Exception e) {
            log.warn("创建通知失败：{}", e.getMessage());
            // 通知创建失败不影响关注操作
        }
        
        log.info("关注成功，当前关注数：{}", watchCount);
        
        return watchCount;
    }
    
    /**
     * 取消关注项目
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 当前关注数
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer unwatchProject(Integer userId, Integer projectId) {
        log.info("取消关注项目，用户 ID: {}, 项目 ID: {}", userId, projectId);
        
        // 1. 查询关注记录
        Watch watch = watchMapper.selectByUserIdAndProjectId(userId, projectId);
        if (watch == null) {
            throw new RuntimeException("您还没有关注该项目");
        }
        
        // 2. 删除关注记录
        watchMapper.deleteById(watch.getId());
        
        // 3. 原子性减少项目关注数
        projectMapper.decrementWatchCount(projectId);
        
        // 4. 查询最新的关注数
        Project project = projectMapper.selectById(projectId);
        Integer watchCount = project != null && project.getWatchCount() != null ? project.getWatchCount() : 0;
        
        log.info("取消关注成功，当前关注数：{}", watchCount);
        
        return watchCount;
    }
    
    /**
     * 获取项目关注数
     * @param projectId 项目 ID
     * @return 关注数
     */
    public Integer getWatchCount(Integer projectId) {
        return watchMapper.countByProjectId(projectId);
    }
}
