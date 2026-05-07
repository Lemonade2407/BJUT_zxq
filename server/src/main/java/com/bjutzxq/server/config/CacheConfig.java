package com.bjutzxq.server.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类（使用 Caffeine）
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 配置缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 配置评论列表缓存：5分钟过期，最大100条
        cacheManager.registerCustomCache("commentList", 
            Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build()
        );
        
        // 配置用户信息缓存：30分钟过期，最大500条
        cacheManager.registerCustomCache("userInfo", 
            Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(500)
                .build()
        );
        
        // 配置项目信息缓存：10分钟过期，最大200条
        cacheManager.registerCustomCache("projectInfo", 
            Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(200)
                .build()
        );
        
        // 配置目录缓存：10分钟过期，最大10000条（用于文件上传时加速目录查找）
        cacheManager.registerCustomCache("directoryCache", 
            Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build()
        );
        
        return cacheManager;
    }
}
