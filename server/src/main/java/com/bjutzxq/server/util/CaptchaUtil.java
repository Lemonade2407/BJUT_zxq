package com.bjutzxq.server.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码工具类
 */
@Slf4j
public class CaptchaUtil {
    
    /**
     * 验证码存储（生产环境应使用 Redis）
     */
    // TODO: 当前使用内存存储，重启后数据丢失，生产环境应迁移到 Redis
    private static final Map<String, CaptchaInfo> CAPTCHA_STORE = new ConcurrentHashMap<>();
    
    /**
     * 验证码有效期（秒）
     */
    private static final int CAPTCHA_EXPIRE_TIME = 300; // 5分钟
    
    /**
     * 验证码宽度
     */
    private static final int WIDTH = 120;
    
    /**
     * 验证码高度
     */
    private static final int HEIGHT = 40;
    
    /**
     * 验证码字符数
     */
    private static final int CODE_LENGTH = 4;
    
    /**
     * 生成图形验证码
     * @param sessionId 会话 ID（用于标识用户）
     * @return Base64 编码的图片数据
     */
    public static String generateCaptcha(String sessionId) {
        log.info("生成图形验证码，会话 ID: {}", sessionId);
        
        // TODO: 添加验证码生成频率限制，防止恶意请求
        // TODO: 考虑使用更复杂的验证码类型（如算术题、滑动验证等）
        
        // 1. 生成随机验证码
        String code = generateRandomCode(CODE_LENGTH);
        
        // 2. 创建验证码图片
        BufferedImage image = createCaptchaImage(code);
        
        // 3. 存储验证码（关联 sessionId）
        CAPTCHA_STORE.put(sessionId, new CaptchaInfo(code, System.currentTimeMillis()));
        
        // 4. 转换为 Base64
        String base64Image = imageToBase64(image);
        
        log.info("图形验证码生成成功，会话 ID: {}", sessionId);
        return base64Image;
    }
    
    /**
     * 验证图形验证码
     * @param sessionId 会话 ID
     * @param userCode 用户输入的验证码
     * @return 是否验证通过
     */
    public static boolean verifyCaptcha(String sessionId, String userCode) {
        log.debug("验证图形验证码，会话 ID: {}", sessionId);
        
        if (sessionId == null || userCode == null) {
            log.warn("验证码验证失败：参数为空");
            return false;
        }
        
        CaptchaInfo captchaInfo = CAPTCHA_STORE.get(sessionId);
        if (captchaInfo == null) {
            log.warn("验证码不存在或已过期，会话 ID: {}", sessionId);
            return false;
        }
        
        // 检查是否过期
        long currentTime = System.currentTimeMillis();
        if (currentTime - captchaInfo.getCreateTime() > CAPTCHA_EXPIRE_TIME * 1000) {
            log.warn("验证码已过期，会话 ID: {}", sessionId);
            CAPTCHA_STORE.remove(sessionId);
            return false;
        }
        
        // 验证验证码（不区分大小写）
        boolean valid = captchaInfo.getCode().equalsIgnoreCase(userCode.trim());
        
        // 验证成功后删除验证码（一次性使用）
        CAPTCHA_STORE.remove(sessionId);
        
        if (valid) {
            log.info("图形验证码验证成功，会话 ID: {}", sessionId);
        } else {
            log.warn("图形验证码错误，会话 ID: {}", sessionId);
        }
        
        return valid;
    }
    
    /**
     * 清除验证码
     * @param sessionId 会话 ID
     */
    public static void clearCaptcha(String sessionId) {
        CAPTCHA_STORE.remove(sessionId);
        log.debug("清除图形验证码，会话 ID: {}", sessionId);
    }
    
    /**
     * 生成随机验证码字符串
     * @param length 长度
     * @return 验证码字符串
     */
    private static String generateRandomCode(int length) {
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 排除易混淆字符
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        
        return code.toString();
    }
    
    /**
     * 创建验证码图片
     * @param code 验证码字符串
     * @return BufferedImage 对象
     */
    private static BufferedImage createCaptchaImage(String code) {
        // 创建图片
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // 设置背景色
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 设置字体
        Font font = new Font("Arial", Font.BOLD, 28);
        g2d.setFont(font);
        
        // 绘制干扰线
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 15; i++) {
            g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // 绘制验证码字符
        int charWidth = WIDTH / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            // 随机颜色
            g2d.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            
            // 随机旋转角度
            double angle = (random.nextDouble() - 0.5) * 0.4;
            
            // 计算位置
            int x = charWidth * (i + 1) - 5;
            int y = HEIGHT / 2 + 10;
            
            // 旋转并绘制字符
            g2d.rotate(angle, x, y);
            g2d.drawString(String.valueOf(code.charAt(i)), x, y);
            g2d.rotate(-angle, x, y);
        }
        
        // 添加噪点
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g2d.fillRect(x, y, 1, 1);
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * 将图片转换为 Base64 字符串
     * @param image BufferedImage 对象
     * @return Base64 编码的字符串
     */
    private static String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("图片转 Base64 失败", e);
            throw new RuntimeException("生成验证码失败");
        }
    }
    
    /**
     * 验证码信息内部类
     */
    private static class CaptchaInfo {
        private String code;
        private long createTime;
        
        public CaptchaInfo(String code, long createTime) {
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
