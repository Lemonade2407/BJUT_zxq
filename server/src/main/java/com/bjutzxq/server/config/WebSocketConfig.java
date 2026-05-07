package com.bjutzxq.server.config;

import com.bjutzxq.server.handler.NotificationWebSocketHandler;
import com.bjutzxq.server.interceptor.WebSocketAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 */
@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private NotificationWebSocketHandler notificationWebSocketHandler;
    
    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("注册 WebSocket 处理器");
        
        // 注册通知 WebSocket 端点
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins(
                    "http://localhost:5173",      // 本地开发环境
                    "http://127.0.0.1:5173",      // 本地开发环境（IP形式）
                    "https://bjut-zxq.cn"         // 生产环境
                );
        
        log.info("WebSocket 端点 /ws/notifications 已注册");
    }
}
