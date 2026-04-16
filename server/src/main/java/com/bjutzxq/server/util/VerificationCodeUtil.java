package com.bjutzxq.server.util;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码工具类（预留接口）
 */
@Slf4j
public class VerificationCodeUtil {
    
    /**
     * 验证码存储（生产环境应使用 Redis）
     */
    private static final Map<String, CodeInfo> CODE_STORE = new ConcurrentHashMap<>();
    
    /**
     * 验证码有效期（秒）
     */
    private static final int CODE_EXPIRE_TIME = 300; // 5分钟
    
    /**
     * 生成随机验证码
     * @param length 验证码长度
     * @return 验证码字符串
     */
    public static String generateCode(int length) {
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        
        return code.toString();
    }
    
    /**
     * 发送邮箱验证码（预留方法）
     * @param email 邮箱地址
     * @return 验证码
     */
    public static String sendEmailCode(String email) {
        log.info("发送邮箱验证码到：{}", email);
        
        // TODO: 集成邮件服务发送验证码
        // 1. 生成验证码
        String code = generateCode(6);
        
        // 2. 存储验证码
        CODE_STORE.put(email, new CodeInfo(code, System.currentTimeMillis()));
        
        // 3. 发送邮件（需要集成邮件服务）
        // emailService.sendVerificationCode(email, code);
        
        log.info("验证码已生成并存储：{}", code);
        return code;
    }
    
    /**
     * 验证邮箱验证码
     * @param email 邮箱地址
     * @param code 用户输入的验证码
     * @return 是否验证通过
     */
    public static boolean verifyEmailCode(String email, String code) {
        log.debug("验证邮箱验证码，邮箱：{}", email);
        
        CodeInfo codeInfo = CODE_STORE.get(email);
        if (codeInfo == null) {
            log.warn("验证码不存在或已过期，邮箱：{}", email);
            return false;
        }
        
        // 检查是否过期
        long currentTime = System.currentTimeMillis();
        if (currentTime - codeInfo.getCreateTime() > CODE_EXPIRE_TIME * 1000) {
            log.warn("验证码已过期，邮箱：{}", email);
            CODE_STORE.remove(email);
            return false;
        }
        
        // 验证验证码
        boolean valid = codeInfo.getCode().equalsIgnoreCase(code);
        if (valid) {
            // 验证成功后删除验证码（一次性使用）
            CODE_STORE.remove(email);
            log.info("验证码验证成功，邮箱：{}", email);
        } else {
            log.warn("验证码错误，邮箱：{}", email);
        }
        
        return valid;
    }
    
    /**
     * 清除验证码
     * @param key 键（邮箱或其他标识）
     */
    public static void clearCode(String key) {
        CODE_STORE.remove(key);
        log.debug("清除验证码，key：{}", key);
    }
    
    /**
     * 验证码信息内部类
     */
    private static class CodeInfo {
        private String code;
        private long createTime;
        
        public CodeInfo(String code, long createTime) {
            this.code = code;
            this.createTime = createTime;
        }
        
        public String getCode() {
            return code;
        }
        
        public long getCreateTime() {
            return createTime;
        }
    }
}
