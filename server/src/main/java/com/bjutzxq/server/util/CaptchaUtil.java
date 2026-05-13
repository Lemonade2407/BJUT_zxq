package com.bjutzxq.server.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CaptchaUtil {

    private static final int CAPTCHA_EXPIRE_TIME = 300;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String REDIS_PREFIX = "captcha:";

    // 测试/降级用的内存存储
    private static final java.util.Map<String, String> FALLBACK = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static CaptchaUtil instance;

    @PostConstruct
    void init() { instance = this; }

    private boolean redisAvailable() {
        return instance != null && instance.redisTemplate != null;
    }

    public static String generateCaptcha(String sessionId) {
        log.info("生成图形验证码，会话 ID: {}", sessionId);
        String code = generateRandomCode(CODE_LENGTH);
        BufferedImage image = createCaptchaImage(code);

        if (instance != null && instance.redisAvailable()) {
            instance.redisTemplate.opsForValue()
                .set(REDIS_PREFIX + sessionId, code, CAPTCHA_EXPIRE_TIME, TimeUnit.SECONDS);
        } else {
            FALLBACK.put(sessionId, code);
        }
        return imageToBase64(image);
    }

    public static boolean verifyCaptcha(String sessionId, String userCode) {
        if (sessionId == null || userCode == null) return false;

        String code;
        if (instance != null && instance.redisAvailable()) {
            code = instance.redisTemplate.opsForValue().get(REDIS_PREFIX + sessionId);
            if (code != null) instance.redisTemplate.delete(REDIS_PREFIX + sessionId);
        } else {
            code = FALLBACK.remove(sessionId);
        }

        if (code == null) {
            log.warn("验证码不存在或已过期，会话 ID: {}", sessionId);
            return false;
        }
        boolean valid = code.equalsIgnoreCase(userCode.trim());
        log.info("图形验证码验证{}，会话 ID: {}", valid ? "成功" : "失败", sessionId);
        return valid;
    }

    public static void clearCaptcha(String sessionId) {
        if (instance != null && instance.redisAvailable()) {
            instance.redisTemplate.delete(REDIS_PREFIX + sessionId);
        } else {
            FALLBACK.remove(sessionId);
        }
    }

    private static String generateRandomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private static BufferedImage createCaptchaImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        Font font = new Font("Arial", Font.BOLD, 28);
        g2d.setFont(font);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 15; i++) {
            g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g2d.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        int charWidth = WIDTH / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            g2d.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            double angle = (random.nextDouble() - 0.5) * 0.4;
            int x = charWidth * (i + 1) - 5;
            int y = HEIGHT / 2 + 10;
            g2d.rotate(angle, x, y);
            g2d.drawString(String.valueOf(code.charAt(i)), x, y);
            g2d.rotate(-angle, x, y);
        }
        for (int i = 0; i < 100; i++) {
            g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g2d.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
        }
        g2d.dispose();
        return image;
    }

    private static String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            log.error("图片转 Base64 失败", e);
            throw new RuntimeException("生成验证码失败");
        }
    }
}
