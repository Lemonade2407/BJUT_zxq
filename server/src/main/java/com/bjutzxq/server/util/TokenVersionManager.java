package com.bjutzxq.server.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Token 版本管理器
 * 管理每个用户的 token 版本号，用于在密码修改/退出所有设备时吊销所有 token
 */
@Slf4j
@Component
public class TokenVersionManager {

    static final String VERSION_PREFIX = "jwt:user-version:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取当前 token 版本号
     * 如果不存在则初始化为 1
     */
    public long getCurrentVersion(Integer userId) {
        String key = VERSION_PREFIX + userId;
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            redisTemplate.opsForValue().set(key, "1");
            log.debug("初始化用户 {} 的 token 版本号为 1", userId);
            return 1;
        }
        return Long.parseLong(val);
    }

    /**
     * 递增 token 版本号（使所有旧 token 失效）
     * @return 递增后的新版本号
     */
    public long incrementVersion(Integer userId) {
        String key = VERSION_PREFIX + userId;
        Long newVersion = redisTemplate.opsForValue().increment(key);
        log.info("用户 {} 的 token 版本号已递增至 {}", userId, newVersion);
        return newVersion != null ? newVersion : 1;
    }
}
