package com.bjutzxq.server.service;

import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.pojo.entity.Watch;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.mapper.WatchMapper;
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
@DisplayName("关注服务测试")
class WatchServiceTest {

    @Mock
    private WatchMapper watchMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProjectTagService projectTagService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WatchService watchService;

    private Project testProject;
    private User testUser;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId(10);
        testProject.setName("测试项目");
        testProject.setOwnerId(200);
        testProject.setWatchCount(8);

        testUser = new User();
        testUser.setId(100);
        testUser.setUsername("测试用户");
    }

    // ==================== watchProject ====================

    @Test
    @DisplayName("关注项目成功 - 正常流程")
    void watchProject_Success() {
        // Arrange
        when(watchMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);
        when(watchMapper.insert(any(Watch.class))).thenReturn(1);
        when(projectMapper.incrementWatchCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);
        when(userMapper.selectById(100)).thenReturn(testUser);

        // Act
        Integer result = watchService.watchProject(100, 10, 1);

        // Assert
        assertNotNull(result);
        assertEquals(8, result);
        verify(watchMapper).selectByUserIdAndProjectId(100, 10);
        verify(watchMapper).insert(any(Watch.class));
        verify(projectMapper).incrementWatchCount(10);
    }

    @Test
    @DisplayName("关注项目 - notificationType为null默认1")
    void watchProject_NullNotificationType() {
        // Arrange
        when(watchMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);
        when(watchMapper.insert(any(Watch.class))).thenAnswer(invocation -> {
            Watch w = invocation.getArgument(0);
            assertEquals(1, w.getNotificationType());
            return 1;
        });
        when(projectMapper.incrementWatchCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);
        when(userMapper.selectById(100)).thenReturn(testUser);

        // Act
        watchService.watchProject(100, 10, null);

        // Assert
        verify(watchMapper).insert(any(Watch.class));
    }

    @Test
    @DisplayName("关注项目 - 发送通知给项目所有者")
    void watchProject_SendsNotification() {
        // Arrange
        when(watchMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);
        when(watchMapper.insert(any(Watch.class))).thenReturn(1);
        when(projectMapper.incrementWatchCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);
        when(userMapper.selectById(100)).thenReturn(testUser);

        // Act
        watchService.watchProject(100, 10, 1);

        // Assert
        verify(notificationService).createNotification(
                eq(200), eq(100), eq(10),
                eq(NotificationType.WATCH), anyString());
    }

    @Test
    @DisplayName("关注项目 - 给自己项目不发送通知")
    void watchProject_OwnProject_NoNotification() {
        // Arrange
        testProject.setOwnerId(100);
        when(watchMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);
        when(watchMapper.insert(any(Watch.class))).thenReturn(1);
        when(projectMapper.incrementWatchCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);

        // Act
        watchService.watchProject(100, 10, 1);

        // Assert
        verify(notificationService, never()).createNotification(
                anyInt(), anyInt(), anyInt(), any(), anyString());
    }

    @Test
    @DisplayName("关注项目失败 - 已经关注")
    void watchProject_AlreadyWatched() {
        // Arrange
        Watch existing = new Watch();
        existing.setId(1);
        when(watchMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(existing);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchService.watchProject(100, 10, 1));
        assertEquals("您已经关注该项目了", exception.getMessage());
        verify(watchMapper, never()).insert(any(Watch.class));
    }

    // ==================== unwatchProject ====================

    @Test
    @DisplayName("取消关注成功 - 正常流程")
    void unwatchProject_Success() {
        // Arrange
        Watch existing = new Watch();
        existing.setId(1);
        existing.setUserId(100);
        existing.setProjectId(10);
        when(watchMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(existing);
        when(watchMapper.deleteById(1)).thenReturn(1);
        when(projectMapper.decrementWatchCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);

        // Act
        Integer result = watchService.unwatchProject(100, 10);

        // Assert
        assertNotNull(result);
        assertEquals(8, result);
        verify(watchMapper).deleteById(1);
        verify(projectMapper).decrementWatchCount(10);
    }

    @Test
    @DisplayName("取消关注失败 - 未关注该项目")
    void unwatchProject_NotWatched() {
        // Arrange
        when(watchMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchService.unwatchProject(100, 10));
        assertEquals("您还没有关注该项目", exception.getMessage());
    }

    // ==================== getUserWatchedProjects ====================

    @Test
    @DisplayName("获取关注项目列表成功 - 正常流程")
    void getUserWatchedProjects_Success() {
        // Arrange
        when(watchMapper.selectProjectIdsByUserId(100)).thenReturn(List.of(10));
        when(projectMapper.selectByIds(List.of(10))).thenReturn(List.of(testProject));
        when(projectTagService.getProjectTagsBatch(List.of(10))).thenReturn(Map.of(10, List.of()));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(
                createUser(200, "项目作者")));

        // Act
        var result = watchService.getUserWatchedProjects(100);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("项目作者", result.get(0).getAuthor());
        verify(watchMapper).selectProjectIdsByUserId(100);
        verify(projectMapper).selectByIds(List.of(10));
        verify(projectTagService).getProjectTagsBatch(List.of(10));
    }

    @Test
    @DisplayName("获取关注项目列表 - 无关注返回空列表")
    void getUserWatchedProjects_Empty() {
        // Arrange
        when(watchMapper.selectProjectIdsByUserId(100)).thenReturn(List.of());

        // Act
        var result = watchService.getUserWatchedProjects(100);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取关注项目列表 - null projectIds返回空列表")
    void getUserWatchedProjects_NullProjectIds() {
        // Arrange
        when(watchMapper.selectProjectIdsByUserId(100)).thenReturn(null);

        // Act
        var result = watchService.getUserWatchedProjects(100);

        // Assert
        assertTrue(result.isEmpty());
    }

    private User createUser(Integer id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }
}
