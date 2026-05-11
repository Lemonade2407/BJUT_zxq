package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.vo.NotificationVO;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("通知控制器测试")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    // ==================== getMyNotifications ====================

    @Test
    @DisplayName("获取我的通知列表 - 正常流程")
    void getMyNotifications_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(notificationService.getUserNotificationsWithSender(100, 1, 20, null))
                    .thenReturn(List.of(new NotificationVO()));
            when(notificationService.countByUserId(100, null)).thenReturn(1L);

            // Act
            var result = notificationController.getMyNotifications(1, 20, null);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    // ==================== markAsRead ====================

    @Test
    @DisplayName("标记已读成功 - 正常流程")
    void markAsRead_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            doNothing().when(notificationService).markAsRead(1, 100);

            // Act
            Result<Void> result = notificationController.markAsRead(1);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    // ==================== markAllAsRead ====================

    @Test
    @DisplayName("全部标记已读 - 正常流程")
    void markAllAsRead_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(notificationService.markAllAsRead(100)).thenReturn(3);

            // Act
            Result<Map<String, Object>> result = notificationController.markAllAsRead();

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(3, result.getData().get("count"));
        }
    }

    // ==================== batchDeleteNotifications ====================

    @Test
    @DisplayName("批量删除通知 - 正常流程")
    void batchDeleteNotifications_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            Map<String, List<Integer>> params = Map.of("ids", List.of(1, 2));
            doNothing().when(notificationService).batchDeleteNotifications(List.of(1, 2), 100);

            // Act
            Result<Void> result = notificationController.batchDeleteNotifications(params);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    @Test
    @DisplayName("批量删除通知失败 - ID列表为空")
    void batchDeleteNotifications_EmptyIds() {
        // Arrange
        Map<String, List<Integer>> emptyParams = Map.of("ids", List.of());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> notificationController.batchDeleteNotifications(emptyParams));
    }
}
