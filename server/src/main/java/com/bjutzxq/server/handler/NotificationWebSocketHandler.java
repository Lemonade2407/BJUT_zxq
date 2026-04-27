package com.bjutzxq.server.handler;

import com.bjutzxq.server.mapper.NotificationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知 WebSocket 处理器
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    
    // 存储用户 ID 与 WebSocket Session 的映射关系
    private static final Map<Integer, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    // 存储用户最后心跳时间
    private static final Map<Integer, LocalDateTime> lastHeartbeatTimes = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    /**
     * WebSocket 连接建立后调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("WebSocket 连接建立，用户 ID: {}, 当前在线用户数: {}", userId, userSessions.size());
            
            // 发送欢迎消息
            sendWelcomeMessage(session, userId);
        } else {
            log.warn("WebSocket 连接建立失败：无法获取用户 ID");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("无法获取用户信息"));
        }
    }
    
    /**
     * 接收到客户端消息时调用
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Integer userId = getUserIdFromSession(session);
        String payload = message.getPayload();
        
        log.debug("收到来自用户 {} 的消息: {}", userId, payload);
        
        try {
            // 解析消息
            Map<String, Object> clientMessage = objectMapper.readValue(payload, Map.class);
            String messageType = (String) clientMessage.get("type");
            
            if (messageType == null) {
                log.warn("收到无效消息，缺少 type 字段，用户 ID: {}", userId);
                return;
            }
            
            // 根据消息类型处理
            switch (messageType) {
                case "heartbeat":
                    handleHeartbeat(userId);
                    break;
                case "ack":
                    handleNotificationAck(userId, clientMessage);
                    break;
                default:
                    log.warn("未知的消息类型: {}, 用户 ID: {}", messageType, userId);
                    sendErrorMessage(session, "未知的消息类型: " + messageType);
            }
        } catch (Exception e) {
            log.error("处理客户端消息失败，用户 ID: {}", userId, e);
            sendErrorMessage(session, "消息处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理心跳消息
     * @param userId 用户 ID
     */
    private void handleHeartbeat(Integer userId) {
        // 更新最后心跳时间
        lastHeartbeatTimes.put(userId, LocalDateTime.now());
        log.debug("收到用户 {} 的心跳", userId);
        
        // 回复心跳响应
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> heartbeatResponse = Map.of(
                    "type", "heartbeat_ack",
                    "timestamp", System.currentTimeMillis(),
                    "message", "心跳正常"
                );
                String jsonMessage = objectMapper.writeValueAsString(heartbeatResponse);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (IOException e) {
                log.error("发送心跳响应失败，用户 ID: {}", userId, e);
            }
        }
    }
    
    /**
     * 处理通知确认消息
     * @param userId 用户 ID
     * @param message 客户端消息
     */
    private void handleNotificationAck(Integer userId, Map<String, Object> message) {
        Object notificationIdObj = message.get("notificationId");
        
        if (notificationIdObj == null) {
            log.warn("通知确认消息缺少 notificationId，用户 ID: {}", userId);
            sendErrorMessage(userSessions.get(userId), "缺少 notificationId 参数");
            return;
        }
        
        Integer notificationId;
        try {
            // 处理数字类型（可能是 Integer 或 Long）
            if (notificationIdObj instanceof Number) {
                notificationId = ((Number) notificationIdObj).intValue();
            } else {
                notificationId = Integer.parseInt(notificationIdObj.toString());
            }
        } catch (NumberFormatException e) {
            log.error("无效的 notificationId 格式: {}", notificationIdObj);
            sendErrorMessage(userSessions.get(userId), "无效的 notificationId 格式");
            return;
        }
        
        log.info("用户 {} 确认收到通知: {}", userId, notificationId);
        
        try {
            // 验证通知是否属于该用户
            var notification = notificationMapper.selectById(notificationId);
            if (notification == null) {
                log.warn("通知不存在，ID: {}", notificationId);
                sendErrorMessage(userSessions.get(userId), "通知不存在");
                return;
            }
            
            if (!notification.getUserId().equals(userId)) {
                log.warn("用户 {} 无权确认通知 {}", userId, notificationId);
                sendErrorMessage(userSessions.get(userId), "无权操作该通知");
                return;
            }
            
            // 如果已经是已读状态，直接返回成功
            if (notification.getIsRead() != null && notification.getIsRead() == 1) {
                log.debug("通知已是已读状态，ID: {}", notificationId);
            } else {
                // 更新数据库中的已读状态
                notificationMapper.markAsRead(notificationId);
                log.info("通知 {} 已标记为已读", notificationId);
            }
            
            // 发送确认响应
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                Map<String, Object> ackResponse = Map.of(
                    "type", "ack_received",
                    "notificationId", notificationId,
                    "timestamp", System.currentTimeMillis(),
                    "message", "确认成功"
                );
                String jsonMessage = objectMapper.writeValueAsString(ackResponse);
                session.sendMessage(new TextMessage(jsonMessage));
            }
        } catch (Exception e) {
            log.error("处理通知确认失败，用户 ID: {}, 通知 ID: {}", userId, notificationId, e);
            sendErrorMessage(userSessions.get(userId), "确认失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送错误消息
     * @param session WebSocket 会话
     * @param errorMessage 错误信息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> errorResponse = Map.of(
                "type", "error",
                "message", errorMessage,
                "timestamp", System.currentTimeMillis()
            );
            String jsonMessage = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(jsonMessage));
        } catch (IOException e) {
            log.error("发送错误消息失败", e);
        }
    }
    
    /**
     * WebSocket 连接关闭后调用
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            lastHeartbeatTimes.remove(userId); // 清理心跳记录
            log.info("WebSocket 连接关闭，用户 ID: {}, 原因: {}, 当前在线用户数: {}", 
                    userId, status.getReason(), userSessions.size());
        }
    }
    
    /**
     * 传输错误处理
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Integer userId = getUserIdFromSession(session);
        log.error("WebSocket 传输错误，用户 ID: {}", userId, exception);
        
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
    
    /**
     * 向指定用户推送通知
     * @param userId 用户 ID
     * @param notificationData 通知数据
     */
    public void sendNotification(Integer userId, Object notificationData) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(notificationData);
                session.sendMessage(new TextMessage(jsonMessage));
                log.debug("通知推送成功，用户 ID: {}", userId);
            } catch (IOException e) {
                log.error("通知推送失败，用户 ID: {}", userId, e);
            }
        } else {
            log.warn("用户 {} 不在线或会话已关闭，无法推送通知", userId);
        }
    }
    
    /**
     * 向所有在线用户广播消息
     * @param message 消息内容
     */
    public void broadcastMessage(Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);
            
            userSessions.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("广播消息失败", e);
                    }
                }
            });
            
            log.debug("广播消息成功，在线用户数: {}", userSessions.size());
        } catch (IOException e) {
            log.error("广播消息序列化失败", e);
        }
    }
    
    /**
     * 检查用户是否在线
     * @param userId 用户 ID
     * @return 是否在线
     */
    public boolean isUserOnline(Integer userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * 获取在线用户数量
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }
    
    /**
     * 获取用户最后心跳时间
     * @param userId 用户 ID
     * @return 最后心跳时间，如果用户不在线则返回 null
     */
    public LocalDateTime getLastHeartbeatTime(Integer userId) {
        return lastHeartbeatTimes.get(userId);
    }
    
    /**
     * 检查用户是否超时（超过 60 秒未发送心跳）
     * @param userId 用户 ID
     * @return 是否超时
     */
    public boolean isUserTimeout(Integer userId) {
        LocalDateTime lastHeartbeat = lastHeartbeatTimes.get(userId);
        if (lastHeartbeat == null) {
            return true;
        }
        return lastHeartbeat.plusSeconds(60).isBefore(LocalDateTime.now());
    }
    
    /**
     * 从 Session 中获取用户 ID
     */
    private Integer getUserIdFromSession(WebSocketSession session) {
        return (Integer) session.getAttributes().get("userId");
    }
    
    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(WebSocketSession session, Integer userId) {
        try {
            Map<String, Object> welcomeMsg = Map.of(
                "type", "welcome",
                "message", "WebSocket 连接成功",
                "userId", userId,
                "heartbeatInterval", 30000, // 建议心跳间隔 30 秒
                "timestamp", System.currentTimeMillis()
            );
            String jsonMessage = objectMapper.writeValueAsString(welcomeMsg);
            session.sendMessage(new TextMessage(jsonMessage));
            log.info("欢迎消息发送成功，用户 ID: {}", userId);
        } catch (IOException e) {
            log.error("发送欢迎消息失败", e);
        }
    }
}
