package com.bjutzxq.server.service;

import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.entity.Notification;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.handler.NotificationWebSocketHandler;
import com.bjutzxq.server.mapper.NotificationMapper;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.server.mapper.UserMapper;
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
@DisplayName("通知服务测试")
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private NotificationWebSocketHandler webSocketHandler;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private User testSender;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1);
        testNotification.setUserId(100);
        testNotification.setSenderId(200);
        testNotification.setProjectId(10);
        testNotification.setType(1);
        testNotification.setContent("测试通知内容");
        testNotification.setIsRead(0);

        testSender = new User();
        testSender.setId(200);
        testSender.setUsername("发消息的用户");

        testProject = new Project();
        testProject.setId(10);
        testProject.setName("测试项目");
    }

    // ==================== createNotification ====================

    @Test
    @DisplayName("创建通知成功 - 正常流程")
    void createNotification_Success() {
        // Arrange
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1);
            return 1;
        });
        doNothing().when(webSocketHandler).sendNotification(anyInt(), anyMap());
        when(userMapper.selectById(200)).thenReturn(testSender);

        // Act
        notificationService.createNotification(100, 200, 10, NotificationType.COMMENT, "测试通知");

        // Assert
        verify(notificationMapper).insert(any(Notification.class));
        verify(webSocketHandler).sendNotification(eq(100), anyMap());
    }

    @Test
    @DisplayName("创建通知失败 - 接收用户ID为空")
    void createNotification_NullUserId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.createNotification(null, 200, 10,
                        NotificationType.COMMENT, "测试"));
    }

    @Test
    @DisplayName("创建通知失败 - 通知类型为空")
    void createNotification_NullType() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.createNotification(100, 200, 10, null, "测试"));
    }

    @Test
    @DisplayName("创建通知失败 - 通知内容为空")
    void createNotification_EmptyContent() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.createNotification(100, 200, 10,
                        NotificationType.COMMENT, ""));
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.createNotification(100, 200, 10,
                        NotificationType.COMMENT, null));
    }

    @Test
    @DisplayName("创建通知 - WebSocket推送失败不影响创建")
    void createNotification_WebSocketFails() {
        // Arrange
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);
        doThrow(new RuntimeException("WebSocket失败")).when(webSocketHandler)
                .sendNotification(anyInt(), anyMap());

        // Act - 不应抛出异常
        notificationService.createNotification(100, 200, 10, NotificationType.COMMENT, "测试");

        // Assert
        verify(notificationMapper).insert(any(Notification.class));
    }

    // ==================== getUserNotificationsWithSender ====================

    @Test
    @DisplayName("获取用户通知列表成功 - 正常流程")
    void getUserNotificationsWithSender_Success() {
        // Arrange
        when(notificationMapper.selectByUserId(100, null)).thenReturn(List.of(testNotification));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(testSender));
        when(projectMapper.selectByIds(anyList())).thenReturn(List.of(testProject));

        // Act
        var result = notificationService.getUserNotificationsWithSender(100, 1, 20, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationMapper).selectByUserId(100, null);
        verify(userMapper).selectBatchIds(anyList());
        verify(projectMapper).selectByIds(anyList());
    }

    @Test
    @DisplayName("获取用户通知列表 - 空结果")
    void getUserNotificationsWithSender_Empty() {
        // Arrange
        when(notificationMapper.selectByUserId(100, null)).thenReturn(List.of());

        // Act
        var result = notificationService.getUserNotificationsWithSender(100, 1, 20, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取用户通知列表失败 - 用户ID为空")
    void getUserNotificationsWithSender_NullUserId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.getUserNotificationsWithSender(null, 1, 20, null));
    }

    // ==================== getUnreadCount ====================

    @Test
    @DisplayName("获取未读通知数量 - 正常流程")
    void getUnreadCount_Success() {
        // Arrange
        when(notificationMapper.countUnreadByUserId(100)).thenReturn(5);

        // Act
        int result = notificationService.getUnreadCount(100);

        // Assert
        assertEquals(5, result);
    }

    @Test
    @DisplayName("获取未读通知数量失败 - 用户ID为空")
    void getUnreadCount_NullUserId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.getUnreadCount(null));
    }

    // ==================== countByUserId ====================

    @Test
    @DisplayName("统计用户通知总数 - 正常流程")
    void countByUserId_Success() {
        // Arrange
        when(notificationMapper.countByUserId(100)).thenReturn(10);

        // Act
        long result = notificationService.countByUserId(100, null);

        // Assert
        assertEquals(10L, result);
    }

    // ==================== markAsRead ====================

    @Test
    @DisplayName("标记已读成功 - 正常流程")
    void markAsRead_Success() {
        // Arrange
        when(notificationMapper.selectById(1)).thenReturn(testNotification);
        when(notificationMapper.update(any(Notification.class))).thenReturn(1);

        // Act
        notificationService.markAsRead(1, 100);

        // Assert
        assertEquals(1, testNotification.getIsRead());
        verify(notificationMapper).update(testNotification);
    }

    @Test
    @DisplayName("标记已读 - 通知已是已读状态直接返回")
    void markAsRead_AlreadyRead() {
        // Arrange
        testNotification.setIsRead(1);
        when(notificationMapper.selectById(1)).thenReturn(testNotification);

        // Act
        notificationService.markAsRead(1, 100);

        // Assert
        verify(notificationMapper, never()).update(any(Notification.class));
    }

    @Test
    @DisplayName("标记已读失败 - 通知不存在")
    void markAsRead_NotFound() {
        // Arrange
        when(notificationMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead(99, 100));
        assertEquals("通知不存在", exception.getMessage());
    }

    @Test
    @DisplayName("标记已读失败 - 无权限（非本人通知）")
    void markAsRead_NotOwner() {
        // Arrange
        when(notificationMapper.selectById(1)).thenReturn(testNotification);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead(1, 999));
        assertEquals("无权限操作该通知", exception.getMessage());
    }

    @Test
    @DisplayName("标记已读失败 - 参数为空")
    void markAsRead_NullParams() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.markAsRead(null, 100));
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.markAsRead(1, null));
    }

    // ==================== markAllAsRead ====================

    @Test
    @DisplayName("全部标记已读成功 - 正常流程")
    void markAllAsRead_Success() {
        // Arrange
        when(notificationMapper.markAllAsRead(100)).thenReturn(3);

        // Act
        int result = notificationService.markAllAsRead(100);

        // Assert
        assertEquals(3, result);
        verify(notificationMapper).markAllAsRead(100);
    }

    @Test
    @DisplayName("全部标记已读失败 - 用户ID为空")
    void markAllAsRead_NullUserId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.markAllAsRead(null));
    }

    // ==================== deleteNotification ====================

    @Test
    @DisplayName("删除通知成功 - 正常流程")
    void deleteNotification_Success() {
        // Arrange
        when(notificationMapper.selectById(1)).thenReturn(testNotification);
        when(notificationMapper.deleteById(1)).thenReturn(1);

        // Act
        notificationService.deleteNotification(1, 100);

        // Assert
        verify(notificationMapper).selectById(1);
        verify(notificationMapper).deleteById(1);
    }

    @Test
    @DisplayName("删除通知失败 - 通知不存在")
    void deleteNotification_NotFound() {
        // Arrange
        when(notificationMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.deleteNotification(99, 100));
        assertEquals("通知不存在", exception.getMessage());
    }

    @Test
    @DisplayName("删除通知失败 - 无权限")
    void deleteNotification_NotOwner() {
        // Arrange
        when(notificationMapper.selectById(1)).thenReturn(testNotification);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.deleteNotification(1, 999));
        assertEquals("无权限删除该通知", exception.getMessage());
    }

    // ==================== batchDeleteNotifications ====================

    @Test
    @DisplayName("批量删除通知成功 - 正常流程")
    void batchDeleteNotifications_Success() {
        // Arrange
        List<Integer> ids = List.of(1, 2);
        Notification notif2 = new Notification();
        notif2.setId(2);
        notif2.setUserId(100);
        when(notificationMapper.selectByIds(ids)).thenReturn(List.of(testNotification, notif2));
        when(notificationMapper.batchDelete(ids)).thenReturn(1);

        // Act
        notificationService.batchDeleteNotifications(ids, 100);

        // Assert
        verify(notificationMapper).selectByIds(ids);
        verify(notificationMapper).batchDelete(ids);
    }

    @Test
    @DisplayName("批量删除通知失败 - ID列表为空")
    void batchDeleteNotifications_EmptyList() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.batchDeleteNotifications(List.of(), 100));
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.batchDeleteNotifications(null, 100));
    }

    @Test
    @DisplayName("批量删除通知失败 - 部分通知不存在")
    void batchDeleteNotifications_MismatchCount() {
        // Arrange
        List<Integer> ids = List.of(1, 2, 3);
        when(notificationMapper.selectByIds(ids)).thenReturn(List.of(testNotification));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.batchDeleteNotifications(ids, 100));
        assertEquals("部分通知不存在", exception.getMessage());
    }

    @Test
    @DisplayName("批量删除通知失败 - 无权限")
    void batchDeleteNotifications_NotOwner() {
        // Arrange
        List<Integer> ids = List.of(1);
        when(notificationMapper.selectByIds(ids)).thenReturn(List.of(testNotification));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.batchDeleteNotifications(ids, 999));
        assertTrue(exception.getMessage().contains("无权限删除通知"));
    }
}
