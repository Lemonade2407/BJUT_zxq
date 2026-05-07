package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.dto.PageResult;
import com.bjutzxq.pojo.vo.NotificationVO;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.NotificationService;
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
     * 获取我的通知列表（分页）
     * GET /api/notifications?pageNum=1&pageSize=20&isRead=0
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param isRead 是否已读（null-全部，0-未读，1-已读）
     * @return 通知列表
     */
    @GetMapping
    public Result<PageResult<NotificationVO>> getMyNotifications(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "isRead", required = false) Integer isRead) {
        log.info("获取我的通知列表，页码：{}, 每页数量：{}, 是否已读：{}", pageNum, pageSize, isRead);
        
        // 从拦截器获取用户 ID（拦截器已保证 userId 存在）
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 获取通知列表（包含发送者信息）
        List<NotificationVO> notifications = notificationService.getUserNotificationsWithSender(
            userId, pageNum, pageSize, isRead);
        
        // 获取通知总数
        long total = notificationService.countByUserId(userId, isRead);
        
        // 构建分页响应
        PageResult<NotificationVO> response = new PageResult<>(notifications, total, pageNum, pageSize);
        
        log.info("获取通知列表成功，数量：{}, 总数：{}", notifications.size(), total);
        return Result.success("获取通知列表成功", response);
    }
    

    
    /**
     * 将通知标记为已读
     * PUT /api/notifications/{id}/read
     * @param id 通知 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(
            @PathVariable Integer id) {
        log.info("标记通知为已读，通知 ID: {}", id);
            
        // 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
            
        // 标记为已读
        notificationService.markAsRead(id, userId);
            
        log.info("标记通知为已读成功，ID: {}", id);
        return Result.success("标记为已读成功", null);
    }
        
    /**
     * 将所有通知标记为已读
     * PUT /api/notifications/read/all
     * @return 操作结果
     */
    @PutMapping("/read/all")
    public Result<Map<String, Object>> markAllAsRead() {
        log.info("将所有通知标记为已读");
            
        // 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
            
        // 全部标记为已读
        int count = notificationService.markAllAsRead(userId);
            
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
            
        log.info("将所有通知标记为已读成功，数量：{}", count);
        return Result.success("全部标记为已读成功", result);
    }
    

    
    /**
     * 批量删除通知
     * POST /api/notifications/batch-delete
     * @param params 通知 ID 列表
     * @return 操作结果
     */
    @PostMapping("/batch-delete")
    public Result<Void> batchDeleteNotifications(
            @RequestBody Map<String, List<Integer>> params) {
        log.info("批量删除通知，IDs: {}", params.get("ids"));
        
        // 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        List<Integer> ids = params.get("ids");
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的通知");
        }
        
        // 批量删除
        notificationService.batchDeleteNotifications(ids, userId);
        
        log.info("批量删除通知成功，数量：{}", ids.size());
        return Result.success("批量删除通知成功", null);
    }
}
