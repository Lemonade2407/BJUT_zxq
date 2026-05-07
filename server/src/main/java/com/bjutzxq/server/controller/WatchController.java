package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.WatchService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关注控制器
 */
@Slf4j
@RestController
@RequestMapping("/watch")
public class WatchController {
    @Autowired
    private WatchService watchService;
    
    /**
     * 关注项目
     * POST /api/watch/{projectId}
     */
    @PostMapping("/{projectId}")
    public Result<Object> watchProject(
            @PathVariable Integer projectId,
            @RequestBody(required = false) WatchRequest watchRequest) {
        
        log.info("收到关注请求，项目 ID: {}", projectId);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 获取通知类型（默认 1）
        Integer notificationType = watchRequest != null ? watchRequest.getNotificationType() : 1;
        
        // 3. 关注项目
        Integer watchCount = watchService.watchProject(userId, projectId, notificationType);
        
        log.info("关注成功，当前关注数：{}", watchCount);
        return Result.success("关注成功", watchCount);
    }
    
    /**
     * 取消关注项目
     * DELETE /api/watch/{projectId}
     */
    @DeleteMapping("/{projectId}")
    public Result<Object> unwatchProject(@PathVariable Integer projectId) {
        
        log.info("收到取消关注请求，项目 ID: {}", projectId);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 取消关注
        Integer watchCount = watchService.unwatchProject(userId, projectId);
        
        log.info("取消关注成功，当前关注数：{}", watchCount);
        return Result.success("取消成功", watchCount);
    }
    
    /**
     * 获取用户关注的项目列表
     * GET /api/watch/my
     */
    @GetMapping("/my")
    public Result<List<Project>> getMyWatchedProjects() {
        
        log.info("获取用户关注的项目列表");
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 获取关注的项目列表
        List<Project> projects = watchService.getUserWatchedProjects(userId);
        
        log.info("返回 {} 个关注的项目", projects.size());
        return Result.success("获取成功", projects);
    }
    
    /**
     * 关注请求参数
     */
    @Data
    public static class WatchRequest {
        private Integer notificationType;
    }
}
