package com.bjutzxq.server.controller;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.RegisterRequest;
import com.bjutzxq.pojo.User;
import com.bjutzxq.server.service.UserService;
import com.bjutzxq.server.util.CaptchaUtil;
import com.bjutzxq.server.util.JwtUtil;
import com.bjutzxq.server.util.RegistrationRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    // 依赖注入
    @Autowired
    private UserService userService;
    
    /**
     * 用户注册（使用 DTO）
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<User> register(
            @RequestBody RegisterRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress) {
        log.info("收到用户注册请求，用户名：{}", request.getUsername());
        
        // TODO: 添加图形验证码防重放攻击机制
        // TODO: 添加邮箱验证功能，确保邮箱真实性
        
        try {
            // 1. 验证图形验证码
            if (request.getCaptchaSessionId() == null || request.getCaptchaCode() == null) {
                log.warn("注册失败：缺少验证码");
                return Result.error("请输入图形验证码");
            }
            boolean captchaValid = CaptchaUtil.verifyCaptcha(
                request.getCaptchaSessionId(), 
                request.getCaptchaCode()
            );
            if (!captchaValid) {
                log.warn("注册失败：验证码错误");
                return Result.error("验证码错误或已过期");
            }
            
            // 2. 验证密码确认
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                log.warn("注册失败：两次密码不一致");
                return Result.error("两次输入的密码不一致");
            }
            
            // 3. 检查注册频率限制（IP）
            if (ipAddress != null && !ipAddress.isEmpty()) {
                RegistrationRateLimiter.RateLimitResult ipResult = 
                    RegistrationRateLimiter.checkIpLimit(ipAddress);
                if (!ipResult.isAllowed()) {
                    log.warn("注册失败：IP 频率限制 - {}", ipResult.getMessage());
                    return Result.error(ipResult.getMessage());
                }
            }
            
            // 4. 检查注册频率限制（邮箱）
            RegistrationRateLimiter.RateLimitResult emailResult = 
                RegistrationRateLimiter.checkEmailLimit(request.getEmail());
            if (!emailResult.isAllowed()) {
                log.warn("注册失败：邮箱频率限制 - {}", emailResult.getMessage());
                return Result.error(emailResult.getMessage());
            }
            
            // 5. 构建 User 对象
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setStudentId(request.getStudentId());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setSex(request.getSex());
            user.setBio(request.getBio());
            
            // 6. 调用服务层注册
            User registeredUser = userService.register(user);
            registeredUser.setPassword(null); // 不返回密码
            
            log.info("用户注册成功，ID: {}", registeredUser.getId());
            return Result.success("注册成功", registeredUser);
        } catch (IllegalArgumentException e) {
            log.warn("注册参数验证失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("注册失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("注册系统错误：{}", e.getMessage(), e);
            return Result.error("注册失败，请稍后重试");
        }
    }
    
    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        
        log.info("收到登录请求，用户名：{}", username);
        
        // TODO: 添加登录失败次数限制，防止暴力破解
        // TODO: 添加图形验证码（登录失败 N 次后强制要求）
        // TODO: 记录登录 IP 和设备信息，便于安全审计
        
        if (username == null || username.trim().isEmpty()) {
            log.warn("登录失败：用户名为空");
            return Result.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            log.warn("登录失败：密码为空");
            return Result.error("密码不能为空");
        }
        
        try {
            Map<String, Object> loginInfo = userService.login(username.trim(), password);
            log.info("用户登录成功：{}", username);
            return Result.success("登录成功", loginInfo);
        } catch (RuntimeException e) {
            log.warn("登录失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 退出登录
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info("用户退出登录");
        userService.logout();
        return Result.success("退出成功", null);
    }
    
    /**
     * 刷新 Token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("请求刷新 Token");
        
        try {
            if (authorization == null) {
                log.warn("刷新 Token 失败：缺少认证信息");
                return Result.error(401, "缺少认证信息");
            }
            
            String token = authorization;
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 验证旧 Token
            if (!JwtUtil.validateToken(token)) {
                log.warn("刷新 Token 失败：Token 无效");
                return Result.error(401, "Token 无效，请重新登录");
            }
            
            // 生成新 Token
            String newToken = JwtUtil.refreshToken(token);
            long remainingTime = JwtUtil.getTokenRemainingTime(newToken);
            
            log.info("Token 刷新成功，剩余时间: {} 秒", remainingTime);
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", newToken);
            result.put("expiresIn", Constants.JWT.TOKEN_EXPIRE_TIME / 1000);
            
            return Result.success("Token 刷新成功", result);
        } catch (Exception e) {
            log.error("刷新 Token 失败：{}", e.getMessage());
            return Result.error(401, "Token 刷新失败，请重新登录");
        }
    }
    
    /**
     * 获取当前用户信息
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("获取当前用户信息");
        
        try {
            if (authorization == null) {
                log.warn("获取用户信息失败：缺少认证信息");
                return Result.error(401, "缺少认证信息");
            }
            
            String token = authorization;
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 解析 token 获取用户 ID
            Integer userId = JwtUtil.getUserIdFromToken(token);
            log.debug("Token 解析成功，用户 ID: {}", userId);
            
            // 获取用户信息
            User user = userService.getCurrentUser(userId);
            log.info("获取用户信息成功：{}, {}", user.getId(), user.getUsername());
            return Result.success(user);
        } catch (Exception e) {
            log.error("获取用户信息失败：{}", e.getMessage());
            return Result.error(401, "未授权访问");
        }
    }
    
    /**
     * 更新用户信息
     * PUT /api/user/profile
     */
    @PutMapping("/user/profile")
    public Result<User> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody User user) {
        log.debug("更新用户信息");
        
        try {
            if (authorization == null) {
                log.warn("更新用户信息失败：缺少认证信息");
                return Result.error(401, "缺少认证信息");
            }
            
            String token = authorization;
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Integer userId = JwtUtil.getUserIdFromToken(token);
            log.debug("Token 解析成功，用户 ID: {}", userId);
            
            User updatedUser = userService.updateProfile(userId, user);
            log.info("用户信息更新成功：{}", userId);
            return Result.success("更新成功", updatedUser);
        } catch (Exception e) {
            log.error("更新用户信息失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 修改密码
     * PUT /api/user/password
     */
    @PutMapping("/user/password")
    public Result<Void> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> params) {
        log.debug("修改密码请求");
        
        try {
            if (authorization == null) {
                log.warn("修改密码失败：缺少认证信息");
                return Result.error(401, "缺少认证信息");
            }
            
            String token = authorization;
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Integer userId = JwtUtil.getUserIdFromToken(token);
            log.debug("Token 解析成功，用户 ID: {}", userId);
            
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");
            
            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                log.warn("修改密码失败：原密码为空");
                return Result.error("原密码不能为空");
            }
            if (newPassword == null || newPassword.trim().isEmpty()) {
                log.warn("修改密码失败：新密码为空");
                return Result.error("新密码不能为空");
            }
            
            // 验证新密码长度
            if (newPassword.length() < 6 || newPassword.length() > 20) {
                log.warn("修改密码失败：密码长度不符合要求");
                return Result.error("密码长度应为 6-20 位");
            }
            
            userService.changePassword(userId, oldPassword.trim(), newPassword.trim());
            log.info("密码修改成功，用户 ID: {}", userId);
            return Result.success("密码修改成功", null);
        } catch (Exception e) {
            log.error("修改密码失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 上传用户头像
     * POST /api/auth/avatar/upload
     */
    @PostMapping("/avatar/upload")
    public Result<String> uploadAvatar(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {
        
        log.info("收到头像上传请求");
        
        try {
            // 1. 获取当前用户 ID
            String authorization = request.getHeader("Authorization");
            if (authorization == null || authorization.trim().isEmpty()) {
                return Result.error(401, "请先登录");
            }
            
            String token = authorization;
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            if (!JwtUtil.validateToken(token)) {
                return Result.error(401, "Token 无效或已过期");
            }
            
            Integer userId = JwtUtil.getUserIdFromToken(token);
            
            // 2. 调用服务层上传头像
            String avatarUrl = userService.uploadAvatar(userId, file);
            
            log.info("头像上传成功，用户 ID: {}, URL: {}", userId, avatarUrl);
            return Result.success("头像上传成功", avatarUrl);
            
        } catch (IllegalArgumentException e) {
            log.warn("头像上传失败：{}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("头像上传失败：{}", e.getMessage(), e);
            return Result.error(500, "头像上传失败：" + e.getMessage());
        }
    }
}
