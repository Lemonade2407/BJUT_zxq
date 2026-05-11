package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.WatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("关注控制器测试")
class WatchControllerTest {

    @Mock
    private WatchService watchService;

    @InjectMocks
    private WatchController watchController;

    // ==================== watchProject ====================

    @Test
    @DisplayName("关注项目成功 - 正常流程")
    void watchProject_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            WatchController.WatchRequest req = new WatchController.WatchRequest();
            req.setNotificationType(1);
            when(watchService.watchProject(100, 10, 1)).thenReturn(9);

            // Act
            Result<Object> result = watchController.watchProject(10, req);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals("关注成功", result.getMessage());
            assertEquals(9, result.getData());
            verify(watchService).watchProject(100, 10, 1);
        }
    }

    @Test
    @DisplayName("关注项目 - 请求体为null默认通知类型为1")
    void watchProject_NullBody() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(watchService.watchProject(100, 10, 1)).thenReturn(9);

            // Act
            Result<Object> result = watchController.watchProject(10, null);

            // Assert
            assertNotNull(result);
            verify(watchService).watchProject(100, 10, 1);
        }
    }

    // ==================== unwatchProject ====================

    @Test
    @DisplayName("取消关注成功 - 正常流程")
    void unwatchProject_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(watchService.unwatchProject(100, 10)).thenReturn(3);

            // Act
            Result<Object> result = watchController.unwatchProject(10);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals("取消成功", result.getMessage());
            assertEquals(3, result.getData());
        }
    }

    // ==================== getMyWatchedProjects ====================

    @Test
    @DisplayName("获取关注项目列表成功 - 正常流程")
    void getMyWatchedProjects_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            Project project = new Project();
            project.setId(1);
            project.setName("关注的项目");
            when(watchService.getUserWatchedProjects(100)).thenReturn(List.of(project));

            // Act
            Result<List<Project>> result = watchController.getMyWatchedProjects();

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(1, result.getData().size());
        }
    }
}
