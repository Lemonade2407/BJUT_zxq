package com.bjutzxq.server.service;

import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.Project;
import com.bjutzxq.pojo.Star;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.server.mapper.StarMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点赞服务类
 */
@Slf4j
@Service
public class StarService {
    @Autowired
    private StarMapper starMapper;
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 点赞项目
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 当前点赞数
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer starProject(Integer userId, Integer projectId) {
        log.info("点赞项目，用户 ID: {}, 项目 ID: {}", userId, projectId);
        
        // 1. 检查是否已经点过赞
        Star existing = starMapper.selectByUserIdAndProjectId(userId, projectId);
        if (existing != null) {
            throw new RuntimeException("您已经点过赞了");
        }
        
        // 2. 新增点赞记录
        Star star = new Star();
        star.setUserId(userId);
        star.setProjectId(projectId); 
        starMapper.insert(star);
        
        // 3. 原子性增加项目点赞数
        projectMapper.incrementStarCount(projectId);
        
        // 4. 查询最新的点赞数
        Project project = projectMapper.selectById(projectId);
        Integer starCount = project != null && project.getStarCount() != null ? project.getStarCount() : 0;
        
        // 5. 创建通知（通知项目所有者）
        try {
            if (project != null && !project.getOwnerId().equals(userId)) {
                String content = "点赞了你的项目：" + project.getName();
                notificationService.createNotification(
                    project.getOwnerId(), userId, projectId,
                    NotificationType.LIKE.getCode(), content);
            }
        } catch (Exception e) {
            log.warn("创建通知失败：{}", e.getMessage());
            // 通知创建失败不影响点赞操作
        }
        
        log.info("点赞成功，当前点赞数：{}", starCount);
        
        return starCount;
    }
    
    /**
     * 取消点赞项目
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 当前点赞数
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer unstarProject(Integer userId, Integer projectId) {
        log.info("取消点赞项目，用户 ID: {}, 项目 ID: {}", userId, projectId);
        
        // 1. 查询点赞记录
        Star star = starMapper.selectByUserIdAndProjectId(userId, projectId);
        if (star == null) {
            throw new RuntimeException("您还没有点赞该项目");
        }
        
        // 2. 删除点赞记录
        starMapper.deleteById(star.getId());
        
        // 3. 原子性减少项目点赞数
        projectMapper.decrementStarCount(projectId);
        
        // 4. 查询最新的点赞数
        Project project = projectMapper.selectById(projectId);
        Integer starCount = project != null && project.getStarCount() != null ? project.getStarCount() : 0;
        
        log.info("取消点赞成功，当前点赞数：{}", starCount);
        
        return starCount;
    }
    
    /**
     * 获取项目点赞数
     * @param projectId 项目 ID
     * @return 点赞数
     */
    public Integer getStarCount(Integer projectId) {
        return starMapper.countByProjectId(projectId);
    }
}
