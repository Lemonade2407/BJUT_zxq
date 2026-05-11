package com.bjutzxq.server.service;

import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.pojo.entity.Star;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.server.mapper.StarMapper;
import com.bjutzxq.server.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("点赞服务测试")
class StarServiceTest {

    @Mock
    private StarMapper starMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StarService starService;

    private Project testProject;
    private User testUser;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId(10);
        testProject.setName("测试项目");
        testProject.setOwnerId(200);
        testProject.setStarCount(5);

        testUser = new User();
        testUser.setId(100);
        testUser.setUsername("测试用户");
    }

    // ==================== starProject ====================

    @Test
    @DisplayName("点赞项目成功 - 正常流程")
    void starProject_Success() {
        // Arrange
        when(starMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);
        when(starMapper.insert(any(Star.class))).thenReturn(1);
        when(projectMapper.incrementStarCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);
        when(userMapper.selectById(100)).thenReturn(testUser);
        doNothing().when(notificationService).createNotification(
                anyInt(), anyInt(), anyInt(), any(NotificationType.class), anyString());

        // Act
        Integer result = starService.starProject(100, 10);

        // Assert
        assertNotNull(result);
        assertEquals(5, result);
        verify(starMapper).selectByUserIdAndProjectId(100, 10);
        verify(starMapper).insert(any(Star.class));
        verify(projectMapper).incrementStarCount(10);
        verify(projectMapper).selectById(10);
    }

    @Test
    @DisplayName("点赞项目 - 发送通知给项目所有者")
    void starProject_SendsNotification() {
        // Arrange
        when(starMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);
        when(starMapper.insert(any(Star.class))).thenReturn(1);
        when(projectMapper.incrementStarCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);
        when(userMapper.selectById(100)).thenReturn(testUser);

        // Act
        starService.starProject(100, 10);

        // Assert
        verify(notificationService).createNotification(
                eq(200), eq(100), eq(10),
                eq(NotificationType.LIKE), anyString());
    }

    @Test
    @DisplayName("点赞项目 - 给自己项目点赞不发送通知")
    void starProject_OwnProject_NoNotification() {
        // Arrange
        testProject.setOwnerId(100); // 自己拥有该项目
        when(starMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);
        when(starMapper.insert(any(Star.class))).thenReturn(1);
        when(projectMapper.incrementStarCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);

        // Act
        starService.starProject(100, 10);

        // Assert
        verify(notificationService, never()).createNotification(
                anyInt(), anyInt(), anyInt(), any(), anyString());
    }

    @Test
    @DisplayName("点赞项目 - 通知发送失败不影响点赞")
    void starProject_NotificationFails() {
        // Arrange
        when(starMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);
        when(starMapper.insert(any(Star.class))).thenReturn(1);
        when(projectMapper.incrementStarCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);
        when(userMapper.selectById(100)).thenReturn(testUser);
        doThrow(new RuntimeException("通知发送失败")).when(notificationService)
                .createNotification(anyInt(), anyInt(), anyInt(), any(), anyString());

        // Act
        Integer result = starService.starProject(100, 10);

        // Assert - 点赞仍然成功
        assertNotNull(result);
        assertEquals(5, result);
    }

    @Test
    @DisplayName("点赞项目失败 - 已经点过赞")
    void starProject_AlreadyStarred() {
        // Arrange
        Star existing = new Star();
        existing.setId(1);
        existing.setUserId(100);
        existing.setProjectId(10);
        when(starMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(existing);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> starService.starProject(100, 10));
        assertEquals("您已经点过赞了", exception.getMessage());
        verify(starMapper, never()).insert(any(Star.class));
    }

    @Test
    @DisplayName("点赞项目 - 项目starCount为null返回0")
    void starProject_NullStarCount() {
        // Arrange
        testProject.setStarCount(null);
        when(starMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);
        when(starMapper.insert(any(Star.class))).thenReturn(1);
        when(projectMapper.incrementStarCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);
        when(userMapper.selectById(100)).thenReturn(testUser);

        // Act
        Integer result = starService.starProject(100, 10);

        // Assert
        assertEquals(0, result);
    }

    // ==================== unstarProject ====================

    @Test
    @DisplayName("取消点赞成功 - 正常流程")
    void unstarProject_Success() {
        // Arrange
        Star existing = new Star();
        existing.setId(1);
        existing.setUserId(100);
        existing.setProjectId(10);
        when(starMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(existing);
        when(starMapper.deleteById(1)).thenReturn(1);
        when(projectMapper.decrementStarCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);

        // Act
        Integer result = starService.unstarProject(100, 10);

        // Assert
        assertNotNull(result);
        assertEquals(5, result);
        verify(starMapper).selectByUserIdAndProjectId(100, 10);
        verify(starMapper).deleteById(1);
        verify(projectMapper).decrementStarCount(10);
    }

    @Test
    @DisplayName("取消点赞失败 - 未点赞该项目")
    void unstarProject_NotStarred() {
        // Arrange
        when(starMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> starService.unstarProject(100, 10));
        assertEquals("您还没有点赞该项目", exception.getMessage());
        verify(starMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("取消点赞 - 项目starCount为null返回0")
    void unstarProject_NullStarCount() {
        // Arrange
        testProject.setStarCount(null);
        Star existing = new Star();
        existing.setId(1);
        when(starMapper.selectByUserIdAndProjectId(100, 10)).thenReturn(existing);
        when(starMapper.deleteById(1)).thenReturn(1);
        when(projectMapper.decrementStarCount(10)).thenReturn(1);
        when(projectMapper.selectById(10)).thenReturn(testProject);

        // Act
        Integer result = starService.unstarProject(100, 10);

        // Assert
        assertEquals(0, result);
    }
}
