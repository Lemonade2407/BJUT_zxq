package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.server.util.CaptchaUtil;
import com.bjutzxq.server.util.PasswordStrengthUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 验证码和密码强度控制器
 */
@Slf4j
@RestController
@RequestMapping("/captcha")
public class CaptchaController {
    
    /**
     * 获取图形验证码
     * GET /api/captcha/image
     */
    @GetMapping("/image")
    public Result<Map<String, String>> getCaptcha() {
        log.info("请求获取图形验证码");
        
        try {
            // 生成会话 ID
            String sessionId = UUID.randomUUID().toString();
            
            // 生成验证码图片（Base64）
            String base64Image = CaptchaUtil.generateCaptcha(sessionId);
            
            // 返回结果
            Map<String, String> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("image", "data:image/png;base64," + base64Image);
            
            log.info("图形验证码生成成功，会话 ID: {}", sessionId);
            return Result.success("验证码获取成功", result);
        } catch (Exception e) {
            log.error("生成图形验证码失败：{}", e.getMessage(), e);
            return Result.error("生成验证码失败");
        }
    }
    
    /**
     * 验证图形验证码
     * POST /api/captcha/verify
     */
    @PostMapping("/verify")
    public Result<Boolean> verifyCaptcha(@RequestBody Map<String, String> params) {
        String sessionId = params.get("sessionId");
        String code = params.get("code");
        
        log.debug("验证图形验证码，会话 ID: {}", sessionId);
        
        if (sessionId == null || code == null) {
            return Result.error("参数不完整");
        }
        
        boolean valid = CaptchaUtil.verifyCaptcha(sessionId, code);
        
        if (valid) {
            return Result.success("验证成功", true);
        } else {
            return Result.error("验证码错误或已过期");
        }
    }
    
    /**
     * 评估密码强度
     * POST /api/captcha/password-strength
     */
    @PostMapping("/password-strength")
    public Result<Map<String, Object>> checkPasswordStrength(@RequestBody Map<String, String> params) {
        String password = params.get("password");
        
        log.debug("评估密码强度");
        
        if (password == null) {
            return Result.error("密码不能为空");
        }
        
        PasswordStrengthUtil.PasswordStrengthInfo strengthInfo = 
            PasswordStrengthUtil.evaluatePassword(password);
        
        Map<String, Object> result = new HashMap<>();
        result.put("level", strengthInfo.getLevel().name());
        result.put("levelDescription", strengthInfo.getLevelDescription());
        result.put("score", strengthInfo.getScore());
        result.put("suggestion", strengthInfo.getSuggestion());
        result.put("meetsRequirement", PasswordStrengthUtil.meetsMinimumRequirement(password));
        
        return Result.success(result);
    }
}
