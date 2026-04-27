package com.bjutzxq.server.interceptor;

import com.bjutzxq.common.Constants;
import com.bjutzxq.server.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器 - 用于 JWT 认证
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.debug("WebSocket 握手请求");
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // 从请求参数中获取 Token
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (token == null || token.trim().isEmpty()) {
                // 尝试从 Authorization header 获取
                String authHeader = servletRequest.getServletRequest().getHeader(Constants.JWT.TOKEN_HEADER);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
            
            if (token == null || token.trim().isEmpty()) {
                log.warn("WebSocket 握手失败：缺少 Token");
                return false;
            }
            
            // 验证 Token
            if (!JwtUtil.validateToken(token)) {
                log.warn("WebSocket 握手失败：Token 无效或已过期");
                return false;
            }
            
            // 从 Token 中获取用户 ID
            Integer userId = JwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("WebSocket 握手失败：无法从 Token 中获取用户 ID");
                return false;
            }
            
            // 将用户 ID 存入 attributes，供后续使用
            attributes.put("userId", userId);
            log.info("WebSocket 握手成功，用户 ID: {}", userId);
            
            return true;
        }
        
        return false;
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket 握手后异常", exception);
        } else {
            log.debug("WebSocket 握手完成");
        }
    }
}
