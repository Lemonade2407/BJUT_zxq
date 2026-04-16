package com.bjutzxq.server.controller;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.Notification;
import com.bjutzxq.server.service.NotificationService;
import com.bjutzxq.server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;
    
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
     * 获取我的通知列表（分页）
     * GET /api/notifications?pageNum=1&pageSize=20&isRead=0
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param isRead 是否已读（null-全部，0-未读，1-已读）
     * @return 通知列表
     */
    @GetMapping
    public Result<List<Map<String, Object>>> getMyNotifications(
            HttpServletRequest request,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "isRead", required = false) Integer isRead) {
        log.info("获取我的通知列表，页码：{}, 每页数量：{}, 是否已读：{}", pageNum, pageSize, isRead);
        
        try {
            // 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 获取通知列表（包含发送者信息）
            List<Map<String, Object>> notifications = notificationService.getUserNotificationsWithSender(
                userId, pageNum, pageSize, isRead);
            
            log.info("获取通知列表成功，数量：{}", notifications.size());
            return Result.success("获取通知列表成功", notifications);
            
        } catch (Exception e) {
            log.error("获取通知列表失败：{}", e.getMessage());
            return Result.error(500, "获取通知列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取未读通知数量
     * GET /api/notifications/unread/count
     * @return 未读通知数量
     */
    @GetMapping("/unread/count")
    public Result<Map<String, Object>> getUnreadCount(
            HttpServletRequest request) {
        log.debug("获取未读通知数量");
        
        try {
            // 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 获取未读数量
            int count = notificationService.getUnreadCount(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            
            log.info("获取未读通知数量成功：{}", count);
            return Result.success("获取未读通知数量成功", result);
            
        } catch (Exception e) {
            log.error("获取未读通知数量失败：{}", e.getMessage());
            return Result.error(500, "获取未读通知数量失败：" + e.getMessage());
        }
    }
    
    /**
     * 将通知标记为已读
     * PUT /api/notifications/{id}/read
     * @param id 通知 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(
            HttpServletRequest request,
            @PathVariable Integer id) {
        log.info("标记通知为已读，通知 ID: {}", id);
        
        try {
            // 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 标记为已读
            notificationService.markAsRead(id, userId);
            
            log.info("标记通知为已读成功，ID: {}", id);
            return Result.success("标记为已读成功", null);
            
        } catch (Exception e) {
            log.error("标记通知为已读失败：{}", e.getMessage());
            return Result.error(500, "标记为已读失败：" + e.getMessage());
        }
    }
    
    /**
     * 将所有通知标记为已读
     * PUT /api/notifications/read/all
     * @return 操作结果
     */
    @PutMapping("/read/all")
    public Result<Map<String, Object>> markAllAsRead(
            HttpServletRequest request) {
        log.info("将所有通知标记为已读");
        
        try {
            // 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 全部标记为已读
            int count = notificationService.markAllAsRead(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            
            log.info("将所有通知标记为已读成功，数量：{}", count);
            return Result.success("全部标记为已读成功", result);
            
        } catch (Exception e) {
            log.error("将所有通知标记为已读失败：{}", e.getMessage());
            return Result.error(500, "全部标记为已读失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除单个通知
     * DELETE /api/notifications/{id}
     * @param id 通知 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(
            HttpServletRequest request,
            @PathVariable Integer id) {
        log.info("删除通知，通知 ID: {}", id);
        
        try {
            // 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 删除通知
            notificationService.deleteNotification(id, userId);
            
            log.info("删除通知成功，ID: {}", id);
            return Result.success("删除通知成功", null);
            
        } catch (Exception e) {
            log.error("删除通知失败：{}", e.getMessage());
            return Result.error(500, "删除通知失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量删除通知
     * POST /api/notifications/batch-delete
     * @param params 通知 ID 列表
     * @return 操作结果
     */
    @PostMapping("/batch-delete")
    public Result<Void> batchDeleteNotifications(
            HttpServletRequest request,
            @RequestBody Map<String, List<Integer>> params) {
        log.info("批量删除通知，IDs: {}", params.get("ids"));
        
        try {
            // 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            List<Integer> ids = params.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error(400, "请选择要删除的通知");
            }
            
            // 批量删除
            notificationService.batchDeleteNotifications(ids, userId);
            
            log.info("批量删除通知成功，数量：{}", ids.size());
            return Result.success("批量删除通知成功", null);
            
        } catch (Exception e) {
            log.error("批量删除通知失败：{}", e.getMessage());
            return Result.error(500, "批量删除通知失败：" + e.getMessage());
        }
    }
}
