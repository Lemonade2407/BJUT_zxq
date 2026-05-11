package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.StarService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("点赞控制器测试")
class StarControllerTest {

    @Mock
    private StarService starService;

    @InjectMocks
    private StarController starController;

    // ==================== starProject ====================

    @Test
    @DisplayName("点赞项目成功 - 正常流程")
    void starProject_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(starService.starProject(100, 10)).thenReturn(6);

            // Act
            Result<Object> result = starController.starProject(10);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals("点赞成功", result.getMessage());
            assertEquals(6, result.getData());
            verify(starService).starProject(100, 10);
        }
    }

    @Test
    @DisplayName("点赞项目失败 - 服务层抛出异常")
    void starProject_ServiceException() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(starService.starProject(100, 10))
                    .thenThrow(new RuntimeException("您已经点过赞了"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> starController.starProject(10));
            assertEquals("您已经点过赞了", exception.getMessage());
        }
    }

    // ==================== unstarProject ====================

    @Test
    @DisplayName("取消点赞成功 - 正常流程")
    void unstarProject_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(starService.unstarProject(100, 10)).thenReturn(4);

            // Act
            Result<Object> result = starController.unstarProject(10);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals("取消成功", result.getMessage());
            assertEquals(4, result.getData());
        }
    }
}
