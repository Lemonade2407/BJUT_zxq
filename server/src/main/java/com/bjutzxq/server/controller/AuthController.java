package com.bjutzxq.server.controller;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.dto.*;
import com.bjutzxq.pojo.vo.*;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.StatisticsService;
import com.bjutzxq.server.service.UserService;
import com.bjutzxq.server.util.CaptchaUtil;
import com.bjutzxq.server.util.DtoConverter;
import com.bjutzxq.server.util.JwtUtil;
import com.bjutzxq.server.util.RegistrationRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private StatisticsService statisticsService;
    
    /**
     * 用户注册（使用 DTO）
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<User> register(
            @Valid @RequestBody RegisterDTO request) {
        log.info("收到用户注册请求，用户名：{}", request.getUsername());
        
        // 1. 验证图形验证码
        if (request.getCaptchaSessionId() == null || request.getCaptchaCode() == null) {
            throw new IllegalArgumentException("请输入图形验证码");
        }
        boolean captchaValid = CaptchaUtil.verifyCaptcha(
            request.getCaptchaSessionId(), 
            request.getCaptchaCode()
        );
        if (!captchaValid) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        
        // 2. 验证密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        
        // 3. 检查注册频率限制（邮箱）
        RegistrationRateLimiter.RateLimitResult emailResult = 
            RegistrationRateLimiter.checkEmailLimit(request.getEmail());
        if (!emailResult.allowed()) {
            throw new IllegalArgumentException(emailResult.message());
        }
        
        // 4. 构建 User 对象
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmployeeId(request.getEmployeeId());
        user.setRealName(request.getRealName());
        user.setClassName(request.getClassName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setSex(request.getSex());
        user.setBio(request.getBio());
        
        // 5. 设置角色（默认为学生）
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            try {
                user.setRole(com.bjutzxq.common.Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("无效的角色：{}，使用默认角色 USER", request.getRole());
                user.setRole(com.bjutzxq.common.Role.USER);
            }
        } else {
            user.setRole(com.bjutzxq.common.Role.USER);
        }
        
        // 6. 调用服务层注册
        User registeredUser = userService.register(user);
        registeredUser.setPassword(null);
        
        log.info("用户注册成功，ID: {}", registeredUser.getId());
        return Result.success("注册成功", registeredUser);
    }
    
    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        log.info("收到登录请求，用户名：{}", username);
        
        LoginVO loginInfo = userService.login(username.trim(), password);
        log.info("用户登录成功：{}", username);
        return Result.success("登录成功", loginInfo);
    }
    
    /**
     * 退出登录
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info("用户退出登录");
        return Result.success("退出成功", null);
    }
    
    /**
     * 刷新 Token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(
            @RequestHeader(value = "Authorization") String authorization) {
        log.info("请求刷新 Token");
        
        String token = authorization;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证旧 Token
        if (!JwtUtil.validateToken(token)) {
            throw new io.jsonwebtoken.JwtException("Token 无效");
        }
        
        // 生成新 Token
        String newToken = JwtUtil.refreshToken(token);
        
        log.info("Token 刷新成功");
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("expiresIn", Constants.JWT.TOKEN_EXPIRE_TIME / 1000);
        
        return Result.success("Token 刷新成功", result);
    }
    
    /**
     * 获取当前用户信息
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(
            @RequestHeader(value = "Authorization") String authorization) {
        log.debug("获取当前用户信息");
        
        String token = authorization;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 解析 token 获取用户 ID
        Integer userId = JwtUtil.getUserIdFromToken(token);
        log.debug("Token 解析成功，用户 ID: {}", userId);
        
        // 获取用户信息并转换为 VO
        User user = userService.getCurrentUser(userId);
        UserVO response = DtoConverter.toUserVO(user);
        log.info("获取用户信息成功：{}, {}", user.getId(), user.getUsername());
        return Result.success(response);
    }
    
    /**
     * 更新用户信息
     * PUT /api/user/profile
     */
    @PutMapping("/user/profile")
    public Result<UserVO> updateProfile(
            @RequestHeader(value = "Authorization") String authorization,
            @RequestBody java.util.Map<String, Object> body) {
        log.debug("更新用户信息");

        String avatar = (String) body.get("avatar");
        String phone = (String) body.get("phone");
        String sex = (String) body.get("sex");
        String bio = (String) body.get("bio");
        String username = (String) body.get("username");

        // 验证手机号格式（如果提供）
        if (phone != null && !phone.trim().isEmpty()) {
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                throw new IllegalArgumentException("手机号格式不正确");
            }
        }

        String token = authorization;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Integer userId = JwtUtil.getUserIdFromToken(token);
        log.debug("Token 解析成功，用户 ID: {}", userId);

        User updatedUser = userService.updateProfile(userId, avatar, phone, sex, bio, username);
        UserVO response = DtoConverter.toUserVO(updatedUser);
        log.info("用户信息更新成功：{}", userId);
        return Result.success("更新成功", response);
    }
    
    /**
     * 修改密码
     * PUT /api/user/password
     */
    @PutMapping("/user/password")
    public Result<Void> changePassword(
            @RequestHeader(value = "Authorization") String authorization,
            @RequestBody java.util.Map<String, String> body) {
        log.debug("修改密码请求");

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        // 1. 验证参数
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("原密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("新密码不能为空");
        }

        // 2. 验证新密码长度
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new IllegalArgumentException("密码长度应为 6-20 位");
        }

        // 3. 验证新密码强度（必须包含字母和数字）
        if (!newPassword.matches("^(?=.*[a-zA-Z])(?=.*\\d).+$")) {
            throw new IllegalArgumentException("密码必须包含字母和数字");
        }

        String token = authorization;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Integer userId = JwtUtil.getUserIdFromToken(token);
        log.debug("Token 解析成功，用户 ID: {}", userId);

        userService.changePassword(userId, oldPassword.trim(), newPassword.trim());
        log.info("密码修改成功，用户 ID: {}", userId);
        return Result.success("密码修改成功", null);
    }
    
    /**
     * 上传用户头像
     * POST /api/auth/avatar/upload
     */
    @PostMapping("/avatar/upload")
    public Result<String> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        
        log.info("收到头像上传请求");
        
        // 1. 获取当前用户 ID（从拦截器）
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 调用服务层上传头像
        String avatarUrl = userService.uploadAvatar(userId, file);
        
        log.info("头像上传成功，用户 ID: {}, URL: {}", userId, avatarUrl);
        return Result.success("头像上传成功", avatarUrl);
    }
    
    /**
     * 根据 ID 获取用户信息（公开接口）
     * GET /api/auth/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<UserVO> getUserById(@PathVariable Integer userId) {
        log.debug("获取用户信息，用户 ID: {}", userId);
        
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 转换为 VO
        UserVO response = DtoConverter.toUserVO(user);
        log.info("获取用户信息成功：{}, {}", user.getId(), user.getUsername());
        return Result.success(response);
    }
    
    /**
     * 获取当前用户的统计数据
     * GET /api/auth/user/statistics
     */
    @GetMapping("/user/statistics")
    public Result<Map<String, Object>> getMyStatistics() {
        Integer userId = UserIdContext.getCurrentUserId();
        return Result.success(statisticsService.getUserStatistics(userId));
    }
}
