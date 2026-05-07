package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.StarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 点赞控制器
 */
@Slf4j
@RestController
@RequestMapping("/projects/{projectId}")
public class StarController {
    @Autowired
    private StarService starService;
    
    /**
     * 点赞项目
     * POST /api/projects/{projectId}/star
     */
    @PostMapping("/star")
    public Result<Object> starProject(@PathVariable Integer projectId) {
        
        log.info("收到点赞请求，项目 ID: {}", projectId);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 点赞项目
        Integer starCount = starService.starProject(userId, projectId);
        
        log.info("点赞成功，当前点赞数：{}", starCount);
        return Result.success("点赞成功", starCount);
    }
    
    /**
     * 取消点赞项目
     * DELETE /api/projects/{projectId}/star
     */
    @DeleteMapping("/star")
    public Result<Object> unstarProject(@PathVariable Integer projectId) {
        
        log.info("收到取消点赞请求，项目 ID: {}", projectId);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 取消点赞
        Integer starCount = starService.unstarProject(userId, projectId);
        
        log.info("取消点赞成功，当前点赞数：{}", starCount);
        return Result.success("取消成功", starCount);
    }
}
