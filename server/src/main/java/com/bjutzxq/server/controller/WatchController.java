package com.bjutzxq.server.controller;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Result;
import com.bjutzxq.server.service.WatchService;
import com.bjutzxq.server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 关注控制器
 */
@Slf4j
@RestController
@RequestMapping("/projects/{projectId}")
public class WatchController {
    @Autowired
    private WatchService watchService;
    
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
     * 关注项目
     * POST /api/projects/{projectId}/watch
     */
    @PostMapping("/watch")
    public Result<Object> watchProject(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @RequestBody(required = false) WatchRequest watchRequest) {
        
        log.info("收到关注请求，项目 ID: {}", projectId);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 获取通知类型（默认 1）
            Integer notificationType = watchRequest != null ? watchRequest.getNotificationType() : 1;
            
            // 3. 关注项目
            Integer watchCount = watchService.watchProject(userId, projectId, notificationType);
            
            log.info("关注成功，当前关注数：{}", watchCount);
            return Result.success("关注成功", watchCount);
            
        } catch (Exception e) {
            log.error("关注失败：{}", e.getMessage());
            return Result.error(500, "关注失败：" + e.getMessage());
        }
    }
    
    /**
     * 取消关注项目
     * DELETE /api/projects/{projectId}/watch
     */
    @DeleteMapping("/watch")
    public Result<Object> unwatchProject(
            HttpServletRequest request,
            @PathVariable Integer projectId) {
        
        log.info("收到取消关注请求，项目 ID: {}", projectId);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 取消关注
            Integer watchCount = watchService.unwatchProject(userId, projectId);
            
            log.info("取消关注成功，当前关注数：{}", watchCount);
            return Result.success("取消成功", watchCount);
            
        } catch (Exception e) {
            log.error("取消关注失败：{}", e.getMessage());
            return Result.error(500, "取消关注失败：" + e.getMessage());
        }
    }
    
    /**
     * 关注请求参数
     */
    @Data
    public static class WatchRequest {
        private Integer notificationType;
    }
}
