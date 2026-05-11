package com.bjutzxq.server.service;

import com.bjutzxq.server.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("统计服务测试")
class StatisticsServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private CommentMapper commentMapper;
    @Mock private TeamMapper teamMapper;
    @Mock private TeamApplicationMapper teamAppMapper;
    @Mock private StarMapper starMapper;
    @Mock private WatchMapper watchMapper;
    @Mock private ProjectFileMapper fileMapper;
    @Mock private TagMapper tagMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private NotificationMapper notifMapper;

    @InjectMocks
    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        // 设置基本返回值，减少重复代码
        lenient().when(projectMapper.countByUserId(anyInt())).thenReturn(5);
        lenient().when(teamMapper.countByUserId(anyInt())).thenReturn(2);
        lenient().when(watchMapper.countByUserId(anyInt())).thenReturn(8);
        lenient().when(starMapper.countByProjectOwner(anyInt())).thenReturn(15);
        lenient().when(projectMapper.sumDownloadsByUserId(anyInt())).thenReturn(42);
        lenient().when(commentMapper.countByUserId(anyInt())).thenReturn(10);
        lenient().when(fileMapper.countByUserId(anyInt())).thenReturn(7);
        lenient().when(notifMapper.countByUserId(anyInt())).thenReturn(3);
        lenient().when(projectMapper.countByUserIdGroupByType(anyInt())).thenReturn(List.of());
        lenient().when(projectMapper.countByUserIdAndMonth(anyInt(), anyString())).thenReturn(1);
        lenient().when(commentMapper.countByUserIdAndMonth(anyInt(), anyString())).thenReturn(2);
    }

    // ==================== getUserStatistics ====================

    @Test
    @DisplayName("获取用户统计成功 - 正常流程")
    void getUserStatistics_Success() {
        // Act
        Map<String, Object> result = statisticsService.getUserStatistics(100);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("cards"));
        assertTrue(result.containsKey("projectTypes"));
        assertTrue(result.containsKey("monthlyActivity"));

        @SuppressWarnings("unchecked")
        Map<String, Object> cards = (Map<String, Object>) result.get("cards");
        assertEquals(5, cards.get("projects"));
        assertEquals(2, cards.get("teams"));
        assertEquals(8, cards.get("watches"));
        assertEquals(15, cards.get("starsReceived"));
        assertEquals(42, cards.get("downloads"));
        assertEquals(10, cards.get("comments"));
        assertEquals(7, cards.get("files"));
        assertEquals(3, cards.get("notifications"));
    }

    @Test
    @DisplayName("获取用户统计 - 月度活动数据包含6个月")
    void getUserStatistics_MonthlyActivity() {
        // Act
        Map<String, Object> result = statisticsService.getUserStatistics(100);

        // Assert
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> monthly = (List<Map<String, Object>>) result.get("monthlyActivity");
        assertNotNull(monthly);
        assertEquals(6, monthly.size());
        for (Map<String, Object> m : monthly) {
            assertTrue(m.containsKey("month"));
            assertTrue(m.containsKey("projects"));
            assertTrue(m.containsKey("comments"));
        }
    }

    @Test
    @DisplayName("获取用户统计 - 项目类型分布不为空")
    void getUserStatistics_ProjectTypes() {
        // Arrange
        when(projectMapper.countByUserIdGroupByType(100)).thenReturn(List.of(
                Map.of("projectType", "课设项目", "count", 3)));

        // Act
        Map<String, Object> result = statisticsService.getUserStatistics(100);

        // Assert
        assertTrue(result.containsKey("projectTypes"));
    }

    // ==================== getAdminStatistics ====================

    @Test
    @DisplayName("获取管理员统计成功 - 正常流程")
    void getAdminStatistics_Success() {
        // Arrange
        when(userMapper.countAll()).thenReturn(100);
        when(projectMapper.countAll()).thenReturn(50);
        when(teamMapper.countAll(null, null, null)).thenReturn(20);
        when(commentMapper.countAll()).thenReturn(200);
        when(tagMapper.countAll()).thenReturn(30);
        when(courseMapper.countAll()).thenReturn(15);
        when(fileMapper.countAllFiles()).thenReturn(120);
        when(teamAppMapper.countByStatus(0)).thenReturn(5);
        when(userMapper.countByRole()).thenReturn(List.of());
        when(projectMapper.countByType()).thenReturn(List.of());
        when(userMapper.countByMonth(anyString())).thenReturn(3);
        when(projectMapper.countByMonth(anyString())).thenReturn(2);
        when(commentMapper.countByMonth(anyString())).thenReturn(5);
        when(tagMapper.selectHotTags(10)).thenReturn(List.of());

        // Act
        Map<String, Object> result = statisticsService.getAdminStatistics();

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("cards"));
        assertTrue(result.containsKey("userRoles"));
        assertTrue(result.containsKey("projectTypes"));
        assertTrue(result.containsKey("monthlyTrend"));
        assertTrue(result.containsKey("topTags"));

        @SuppressWarnings("unchecked")
        Map<String, Object> cards = (Map<String, Object>) result.get("cards");
        assertEquals(100, cards.get("users"));
        assertEquals(50, cards.get("projects"));
        assertEquals(20, cards.get("teams"));
        assertEquals(200, cards.get("comments"));
        assertEquals(30, cards.get("tags"));
        assertEquals(15, cards.get("courses"));
        assertEquals(120, cards.get("files"));
        assertEquals(5, cards.get("pendingApps"));
    }

    @Test
    @DisplayName("获取管理员统计 - 月度趋势包含6个月")
    void getAdminStatistics_MonthlyTrend() {
        // Arrange
        when(userMapper.countAll()).thenReturn(0);
        when(projectMapper.countAll()).thenReturn(0);
        when(teamMapper.countAll(any(), any(), any())).thenReturn(0);
        when(commentMapper.countAll()).thenReturn(0);
        when(tagMapper.countAll()).thenReturn(0);
        when(courseMapper.countAll()).thenReturn(0);
        when(fileMapper.countAllFiles()).thenReturn(0);
        when(teamAppMapper.countByStatus(anyInt())).thenReturn(0);
        when(userMapper.countByRole()).thenReturn(List.of());
        when(projectMapper.countByType()).thenReturn(List.of());
        when(tagMapper.selectHotTags(anyInt())).thenReturn(List.of());

        // Act
        Map<String, Object> result = statisticsService.getAdminStatistics();

        // Assert
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> monthly = (List<Map<String, Object>>) result.get("monthlyTrend");
        assertNotNull(monthly);
        assertEquals(6, monthly.size());
        for (Map<String, Object> m : monthly) {
            assertTrue(m.containsKey("month"));
            assertTrue(m.containsKey("users"));
            assertTrue(m.containsKey("projects"));
            assertTrue(m.containsKey("comments"));
        }
    }
}
