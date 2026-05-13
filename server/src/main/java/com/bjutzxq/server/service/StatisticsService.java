package com.bjutzxq.server.service;

import com.bjutzxq.server.mapper.*;
import com.bjutzxq.server.mapper.CourseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StatisticsService {

    @Autowired private UserMapper userMapper;
    @Autowired private ProjectMapper projectMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private TeamMapper teamMapper;
    @Autowired private TeamApplicationMapper teamAppMapper;
    @Autowired private StarMapper starMapper;
    @Autowired private WatchMapper watchMapper;
    @Autowired private ProjectFileMapper fileMapper;
    @Autowired private TagMapper tagMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private NotificationMapper notifMapper;
    @Autowired private StringRedisTemplate redisTemplate;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 缓存键前缀
    private static final String USER_STATS_PREFIX = "stats:user:";
    private static final String ADMIN_STATS_PREFIX = "stats:admin";
    // 缓存过期时间：5分钟
    private static final long CACHE_TTL_MINUTES = 5;

    // ==================== 个人统计 ====================

    public Map<String, Object> getUserStatistics(Integer userId) {
        String cacheKey = USER_STATS_PREFIX + userId;
        
        // 1. 尝试从缓存读取
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("命中用户统计缓存，userId: {}", userId);
                return objectMapper.readValue(cached, Map.class);
            }
        } catch (Exception e) {
            log.warn("读取用户统计缓存失败: {}", e.getMessage());
        }
        
        // 2. 缓存未命中，查询数据库
        log.info("查询用户统计数据，userId: {}", userId);
        Map<String, Object> stats = buildUserStatistics(userId);
        
        // 3. 写入缓存
        try {
            String json = objectMapper.writeValueAsString(stats);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("用户统计已缓存，userId: {}", userId);
        } catch (Exception e) {
            log.warn("写入用户统计缓存失败: {}", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 构建用户统计数据（内部方法）
     */
    private Map<String, Object> buildUserStatistics(Integer userId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 概览卡片
        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("projects", projectMapper.countByUserId(userId));
        cards.put("teams", teamMapper.countByUserId(userId));
        cards.put("watches", watchMapper.countByUserId(userId));
        cards.put("starsReceived", starMapper.countByProjectOwner(userId));
        cards.put("downloads", projectMapper.sumDownloadsByUserId(userId));
        cards.put("comments", commentMapper.countByUserId(userId));
        cards.put("files", fileMapper.countByUserId(userId));
        cards.put("notifications", notifMapper.countByUserId(userId));
        stats.put("cards", cards);

        // 项目类型分布
        stats.put("projectTypes", projectMapper.countByUserIdGroupByType(userId));

        // 月度活跃趋势（近6月：创建项目+评论）
        List<Map<String, Object>> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String month = LocalDate.now().minusMonths(i).format(MONTH_FMT);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", month);
            m.put("projects", projectMapper.countByUserIdAndMonth(userId, month));
            m.put("comments", commentMapper.countByUserIdAndMonth(userId, month));
            monthly.add(m);
        }
        stats.put("monthlyActivity", monthly);

        return stats;
    }

    // ==================== 管理员统计 ====================

    public Map<String, Object> getAdminStatistics() {
        String cacheKey = ADMIN_STATS_PREFIX;
        
        // 1. 尝试从缓存读取
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("命中管理员统计缓存");
                return objectMapper.readValue(cached, Map.class);
            }
        } catch (Exception e) {
            log.warn("读取管理员统计缓存失败: {}", e.getMessage());
        }
        
        // 2. 缓存未命中，查询数据库
        log.info("查询管理员统计数据");
        Map<String, Object> stats = buildAdminStatistics();
        
        // 3. 写入缓存
        try {
            String json = objectMapper.writeValueAsString(stats);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("管理员统计已缓存");
        } catch (Exception e) {
            log.warn("写入管理员统计缓存失败: {}", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 构建管理员统计数据（内部方法）
     */
    private Map<String, Object> buildAdminStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 总览卡片
        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("users", userMapper.countAll());
        cards.put("projects", projectMapper.countAll());
        cards.put("teams", teamMapper.countAll(null, null, null));
        cards.put("comments", commentMapper.countAll());
        cards.put("tags", tagMapper.countAll());
        cards.put("courses", courseMapper.countAll()); // using a simple count
        cards.put("files", fileMapper.countAllFiles());
        cards.put("pendingApps", teamAppMapper.countByStatus(0));
        stats.put("cards", cards);

        // 用户角色分布
        stats.put("userRoles", userMapper.countByRole());

        // 项目类型分布
        stats.put("projectTypes", projectMapper.countByType());

        // 月度趋势（近6月：用户+项目+评论）
        List<Map<String, Object>> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String month = LocalDate.now().minusMonths(i).format(MONTH_FMT);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", month);
            m.put("users", userMapper.countByMonth(month));
            m.put("projects", projectMapper.countByMonth(month));
            m.put("comments", commentMapper.countByMonth(month));
            monthly.add(m);
        }
        stats.put("monthlyTrend", monthly);

        // 热门标签 Top 10
        stats.put("topTags", tagMapper.selectHotTags(10));

        return stats;
    }
}
