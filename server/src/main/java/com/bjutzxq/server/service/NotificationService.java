package com.bjutzxq.server.service;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.vo.NotificationVO;
import com.bjutzxq.pojo.entity.Notification;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.server.mapper.NotificationMapper;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.server.util.DtoConverter;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bjutzxq.server.handler.NotificationWebSocketHandler;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知服务类
 */
@Slf4j
@Service
public class NotificationService {
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private NotificationWebSocketHandler webSocketHandler;
    

    /**
     * 创建通知
     * @param userId 接收用户 ID
     * @param senderId 发送用户 ID
     * @param projectId 相关项目 ID
     * @param type 通知类型枚举
     * @param content 通知内容
     */
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Integer userId, Integer senderId, Integer projectId, 
                                  NotificationType type, String content) {
        log.info("创建通知，接收用户 ID: {}, 类型：{}", userId, type);
        
        // 参数验证
        if (userId == null) {
            throw new IllegalArgumentException("接收用户 ID 不能为空");
        }
        if (type == null) {
            throw new IllegalArgumentException("通知类型不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("通知内容不能为空");
        }
        
        // 创建通知对象
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(senderId);
        notification.setProjectId(projectId);
        notification.setType(type.getCode());  // 存储枚举的 code 值
        notification.setContent(content.trim());
        notification.setIsRead(0); // 默认为未读
        notification.setCreatedAt(LocalDateTime.now());
        
        // 保存到数据库
        notificationMapper.insert(notification);
        
        log.info("通知创建成功，ID: {}", notification.getId());
        
        // 通过 WebSocket 实时推送通知
        try {
            pushNotificationViaWebSocket(notification);
        } catch (Exception e) {
            // WebSocket 推送失败不应影响通知创建
            log.error("WebSocket 推送通知失败，但不影响通知创建", e);
        }
    }

    /**
     * 获取用户的通知列表（包含发送者信息）
     * @param userId 用户 ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param isRead 是否已读（null-全部，0-未读，1-已读）
     * @return 通知列表（包含发送者信息的 NotificationVO）
     */
    public List<NotificationVO> getUserNotificationsWithSender(
            Integer userId, Integer pageNum, Integer pageSize, Integer isRead) {
        log.info("获取用户通知列表（含发送者信息），用户 ID: {}, 页码：{}, 每页数量：{}, 是否已读：{}", 
                   userId, pageNum, pageSize, isRead);
        
        // 参数验证
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        // 分页参数验证
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 20; // 默认每页 20 条，最多 100 条
        }
        
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);
        
        // 查询通知列表
        List<Notification> notifications = notificationMapper.selectByUserId(userId, isRead);
        
        if (notifications.isEmpty()) {
            return List.of();
        }
        
        // 批量查询所有发送者信息（优化 N+1 查询）
        Set<Integer> senderIds = notifications.stream()
            .map(Notification::getSenderId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Map<Integer, User> userMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(new ArrayList<>(senderIds));
            userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
            log.debug("批量查询发送者信息，共 {} 个用户", userMap.size());
        }
        
        // 批量查询所有项目名称（优化 N+1 查询）
        Set<Integer> projectIds = notifications.stream()
            .map(Notification::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Map<Integer, String> projectNameMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            List<Project> projects = projectMapper.selectByIds(new ArrayList<>(projectIds));
            projectNameMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
            log.debug("批量查询项目信息，共 {} 个项目", projectNameMap.size());
        }
        
        // 构建包含发送者和项目信息的结果列表
        List<NotificationVO> result = new java.util.ArrayList<>();
        for (Notification notification : notifications) {
            // 获取发送者用户名
            String senderUsername = null;
            if (notification.getSenderId() != null) {
                User sender = userMap.get(notification.getSenderId());
                if (sender != null) {
                    senderUsername = sender.getUsername();
                }
            }

            // 获取项目名称
            String projectName = null;
            if (notification.getProjectId() != null) {
                projectName = projectNameMap.get(notification.getProjectId());
            }
            
            // 转换为 NotificationVO
            NotificationVO vo = 
                DtoConverter.toNotificationVO(notification, senderUsername, projectName);
            result.add(vo);
        }
        
        log.info("通知列表获取成功（含发送者信息），数量：{}", result.size());
        
        return result;
    }
    
    /**
     * 获取用户的未读通知数量
     * @param userId 用户 ID
     * @return 未读通知数量
     */
    public int getUnreadCount(Integer userId) {
        log.debug("获取未读通知数量，用户 ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        int count = notificationMapper.countUnreadByUserId(userId);
        log.debug("未读通知数量：{}", count);
        
        return count;
    }
    
    /**
     * 统计用户的通知总数
     * @param userId 用户 ID
     * @param isRead 是否已读（null-全部，0-未读，1-已读）
     * @return 通知总数
     */
    public long countByUserId(Integer userId, Integer isRead) {
        log.debug("统计通知总数，用户 ID: {}, 是否已读: {}", userId, isRead);
        if (userId == null) throw new IllegalArgumentException("用户 ID 不能为空");
        // 直接 COUNT 查询，避免重复执行 SELECT * 查询
        return notificationMapper.countByUserId(userId);
    }
    
    /**
     * 将通知标记为已读
     * @param notificationId 通知 ID
     * @param userId 用户 ID（用于权限验证）
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Integer notificationId, Integer userId) {
        log.info("标记通知为已读，通知 ID: {}, 用户 ID: {}", notificationId, userId);
        
        // 参数验证
        if (notificationId == null) {
            throw new IllegalArgumentException("通知 ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        // 查询通知信息
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        
        // 验证权限（只能标记自己的通知）
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作该通知");
        }
        
        // 如果已经是已读状态，直接返回
        if (notification.getIsRead() != null && Constants.Notification.READ_READ.equals(notification.getIsRead())) {
            log.debug("通知已是已读状态，ID: {}", notificationId);
            return;
        }
        
        // 标记为已读
        notification.setIsRead(1);
        notificationMapper.update(notification);
        
        log.info("通知标记为已读成功，ID: {}", notificationId);
    }
    
    /**
     * 将用户的所有通知标记为已读
     * @param userId 用户 ID
     * @return 已读的通知数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int markAllAsRead(Integer userId) {
        log.info("将所有通知标记为已读，用户 ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        int count = notificationMapper.markAllAsRead(userId);
        log.info("标记通知为已读完成，数量：{}", count);
        
        return count;
    }
    
    /**
     * 删除通知
     * @param notificationId 通知 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Integer notificationId, Integer userId) {
        log.info("删除通知，通知 ID: {}, 用户 ID: {}", notificationId, userId);
        
        // 参数验证
        if (notificationId == null) {
            throw new IllegalArgumentException("通知 ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        // 查询通知信息
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        
        // 验证权限（只能删除自己的通知）
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除该通知");
        }
        
        // 删除通知
        notificationMapper.deleteById(notificationId);
        
        log.info("通知删除成功，ID: {}", notificationId);
    }
    
    /**
     * 批量删除通知
     * @param notificationIds 通知 ID 列表
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteNotifications(List<Integer> notificationIds, Integer userId) {
        log.info("批量删除通知，通知 IDs: {}, 用户 ID: {}", notificationIds, userId);
        
        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new IllegalArgumentException("通知 ID 列表不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        // 批量查询验证所有权
        List<Notification> notifications = notificationMapper.selectByIds(notificationIds);
        if (notifications.size() != notificationIds.size()) {
            throw new RuntimeException("部分通知不存在");
        }
        for (Notification n : notifications) {
            if (!n.getUserId().equals(userId)) {
                throw new RuntimeException("无权限删除通知：" + n.getId());
            }
        }
        // 批量删除
        notificationMapper.batchDelete(notificationIds);
        
        log.info("批量删除通知成功，数量：{}", notificationIds.size());
    }
    
    /**
     * 通过 WebSocket 推送通知
     * @param notification 通知对象
     */
    private void pushNotificationViaWebSocket(Notification notification) {
        if (notification.getUserId() == null) {
            log.warn("通知接收用户 ID 为空，跳过 WebSocket 推送");
            return;
        }
        
        // 构建推送数据
        Map<String, Object> pushData = new HashMap<>();
        pushData.put("type", "notification");
        pushData.put("id", notification.getId());
        pushData.put("userId", notification.getUserId());
        pushData.put("senderId", notification.getSenderId());
        pushData.put("projectId", notification.getProjectId());
        pushData.put("notificationType", notification.getType());
        pushData.put("content", notification.getContent());
        pushData.put("isRead", notification.getIsRead());
        pushData.put("createdAt", notification.getCreatedAt());
        
        // 获取发送者信息
        if (notification.getSenderId() != null) {
            User sender = userMapper.selectById(notification.getSenderId());
            if (sender != null) {
                Map<String, Object> senderInfo = new HashMap<>();
                senderInfo.put("id", sender.getId());
                senderInfo.put("username", sender.getUsername());
                pushData.put("sender", senderInfo);
            }
        }
        
        // 推送通知
        webSocketHandler.sendNotification(notification.getUserId(), pushData);
        
        log.debug("已通过 WebSocket 推送通知给用户 {}", notification.getUserId());
    }
}
