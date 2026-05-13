package com.bjutzxq.server.interceptor;

import com.bjutzxq.common.Result;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 用户ID提取拦截器
 */
@Slf4j
public class JwtUserIdInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 优先从 Cookie 读取 Token，兼容 Authorization header
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("auth_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }
        }

        if (token != null) {
            
            try {
                // 验证 Token 并获取用户ID
                if (JwtUtil.validateToken(token)) {
                    Integer userId = JwtUtil.getUserIdFromToken(token);
                    
                    // 将用户ID存入上下文
                    UserIdContext.setCurrentUserId(userId);
                } else {
                    log.warn("Token 无效, path={}", request.getRequestURI());
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(Result.error("无效的 Token").toString());
                    return false;
                }
            } catch (Exception e) {
                log.error("解析 Token 失败, path={}", request.getRequestURI(), e);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(Result.error("Token 解析失败").toString());
                return false;
            }
        } else {
            // 检查是否是公开接口（不需要登录）
            String path = request.getRequestURI();
            boolean isPublicPath = path.equals("/api/projects/public") || 
                                   path.startsWith("/api/auth/");
            
            if (!isPublicPath) {
                log.warn("未找到有效的 Token，拒绝访问: path={}", path);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(Result.error("请先登录").toString());
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理上下文，防止内存泄漏
        UserIdContext.clear();
        log.debug("用户上下文已清理");
    }
}
