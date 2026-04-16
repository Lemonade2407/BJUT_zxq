package com.bjutzxq.server.controller;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Result;
import com.bjutzxq.server.service.StarService;
import com.bjutzxq.server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
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
     * 从 Authorization header 中解析 Token 获取当前登录用户 ID
     */
    private Integer getCurrentUserId(HttpServletRequest request) {
        String authorization = request.getHeader(Constants.JWT.TOKEN_HEADER);
        
        if (authorization == null || authorization.trim().isEmpty()) {
            throw new RuntimeException("请先登录");
        }
        
        // 提取 Token（去掉 "Bearer " 前缀）
        String token = authorization;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证并解析 Token
        if (!JwtUtil.validateToken(token)) {
            throw new RuntimeException("Token 无效或已过期");
        }
        
        return JwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 点赞项目
     * POST /api/projects/{projectId}/star
     */
    @PostMapping("/star")
    public Result<Object> starProject(
            HttpServletRequest request,
            @PathVariable Integer projectId) {
        
        log.info("收到点赞请求，项目 ID: {}", projectId);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 点赞项目
            Integer starCount = starService.starProject(userId, projectId);
            
            log.info("点赞成功，当前点赞数：{}", starCount);
            return Result.success("点赞成功", starCount);
            
        } catch (Exception e) {
            log.error("点赞失败：{}", e.getMessage());
            return Result.error(500, "点赞失败：" + e.getMessage());
        }
    }
    
    /**
     * 取消点赞项目
     * DELETE /api/projects/{projectId}/star
     */
    @DeleteMapping("/star")
    public Result<Object> unstarProject(
            HttpServletRequest request,
            @PathVariable Integer projectId) {
        
        log.info("收到取消点赞请求，项目 ID: {}", projectId);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 取消点赞
            Integer starCount = starService.unstarProject(userId, projectId);
            
            log.info("取消点赞成功，当前点赞数：{}", starCount);
            return Result.success("取消成功", starCount);
            
        } catch (Exception e) {
            log.error("取消点赞失败：{}", e.getMessage());
            return Result.error(500, "取消点赞失败：" + e.getMessage());
        }
    }
}
