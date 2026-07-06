package com.bjutzxq.server.aspect;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Role;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.annotation.RequireRole;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import java.lang.reflect.Method;

/**
 * 权限检查切面
 */
@Slf4j  // Lombok 自动生成 log 对象
@Aspect//声明是切片类
@Component//交给 Spring 管理
@Order(1) // 确保在事务之前执行
public class PermissionAspect {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 环绕通知：拦截带有 @RequireRole 注解的方法
     */
    @Around("@annotation(com.bjutzxq.server.annotation.RequireRole)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        
        if (requireRole == null) {
            // 没有注解，直接执行
            return joinPoint.proceed();
        }
        
        // 优先从 Cookie 读取 Token，兼容 Authorization header
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        
        String token = null;
        
        // 1. 尝试从 Cookie 读取
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("auth_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        
        // 2. 如果 Cookie 中没有，尝试从 Authorization header 读取
        if (token == null || token.isEmpty()) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            } else if (authorization != null) {
                token = authorization;
            }
        }
        
        if (token == null || token.trim().isEmpty()) {
            log.warn("权限验证失败：缺少认证信息");
            throw new RuntimeException("未授权访问");
        }
        
        Integer userId;
        try {
            userId = JwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("权限验证失败：Token 无效，错误: {}", e.getMessage());
            throw new RuntimeException("Token 无效");
        }
        
        // 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("权限验证失败：用户不存在，ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户状态
        if (!Constants.User.STATUS_NORMAL.equals(user.getStatus())) {
            log.warn("权限验证失败：用户已被禁用，ID: {}", userId);
            throw new RuntimeException("账号已被禁用");
        }
        
        // 检查用户角色
        Role userRole = user.getRole();
        if (userRole == null) {
            log.warn("权限验证失败：用户角色为空，ID: {}", userId);
            throw new RuntimeException("用户角色异常");
        }
        
        // 验证角色权限
        Role[] requiredRoles = requireRole.value();
        boolean hasPermission = false;
        for (Role required : requiredRoles) {
            if (userRole == required) {
                hasPermission = true;
                break;
            }
        }
        
        if (!hasPermission) {
            log.warn("权限不足：用户角色={}, 需要的角色={}, 用户 ID={}", 
                       userRole, requiredRoles, userId);
            throw new RuntimeException(requireRole.message());
        }
        
        log.info("权限验证通过：用户={}, 角色={}, 方法={}", 
                   user.getUsername(), userRole, method.getName());
        
        // 权限验证通过，执行目标方法
        return joinPoint.proceed();
    }
}
