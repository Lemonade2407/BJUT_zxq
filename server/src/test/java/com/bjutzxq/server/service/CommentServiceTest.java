package com.bjutzxq.server.service;

import com.bjutzxq.common.BusinessException;
import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.entity.Comment;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.mapper.CommentMapper;
import com.bjutzxq.server.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("评论服务测试")
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    private Comment testComment;
    private User testUser;
    private Map<String, Object> testUserMap;

    @BeforeEach
    void setUp() {
        testComment = new Comment();
        testComment.setId(1);
        testComment.setUserId(100);
        testComment.setProjectId(10);
        testComment.setContent("好项目");
        testComment.setLikeCount(0);
        testComment.setStatus(1);

        testUser = new User();
        testUser.setId(100);
        testUser.setUsername("测试用户");

        testUserMap = new HashMap<>();
        testUserMap.put("id", 100);
        testUserMap.put("username", "测试用户");
        testUserMap.put("avatar", "https://example.com/avatar.png");
    }

    // ==================== postComment ====================

    @Test
    @DisplayName("发表评论成功 - 正常流程")
    void postComment_Success() {
        // Arrange
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(1);
            return 1;
        });
        when(commentMapper.getProjectOwnerId(10)).thenReturn(200);
        when(commentMapper.getProjectName(10)).thenReturn("测试项目");
        when(userMapper.selectById(100)).thenReturn(testUser);

        // Act
        Comment result = commentService.postComment(100, 10, "好项目");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("好项目", result.getContent());
        assertEquals(100, result.getUserId());
        assertEquals(1, result.getStatus());
        verify(commentMapper).insert(any(Comment.class));
    }

    @Test
    @DisplayName("发表评论 - 发送通知给项目所有者")
    void postComment_SendsNotification() {
        // Arrange
        when(commentMapper.insert(any(Comment.class))).thenReturn(1);
        when(commentMapper.getProjectOwnerId(10)).thenReturn(200);
        when(commentMapper.getProjectName(10)).thenReturn("测试项目");
        when(userMapper.selectById(100)).thenReturn(testUser);

        // Act
        commentService.postComment(100, 10, "好项目");

        // Assert
        verify(notificationService).createNotification(
                eq(200), eq(100), eq(10),
                eq(NotificationType.COMMENT), anyString());
    }

    @Test
    @DisplayName("发表评论 - 给自己的项目评论不发送通知")
    void postComment_OwnProject_NoNotification() {
        // Arrange
        when(commentMapper.insert(any(Comment.class))).thenReturn(1);
        when(commentMapper.getProjectOwnerId(10)).thenReturn(100); // 自己拥有该项目
        when(commentMapper.getProjectName(10)).thenReturn("测试项目");

        // Act
        commentService.postComment(100, 10, "好项目");

        // Assert
        verify(notificationService, never()).createNotification(
                anyInt(), anyInt(), anyInt(), any(), anyString());
    }

    @Test
    @DisplayName("发表评论 - 通知发送失败不影响评论")
    void postComment_NotificationFails() {
        // Arrange
        when(commentMapper.insert(any(Comment.class))).thenReturn(1);
        when(commentMapper.getProjectOwnerId(10)).thenReturn(200);
        when(commentMapper.getProjectName(10)).thenReturn("测试项目");
        when(userMapper.selectById(100)).thenReturn(testUser);
        doThrow(new RuntimeException("通知失败")).when(notificationService)
                .createNotification(anyInt(), anyInt(), anyInt(), any(), anyString());

        // Act
        Comment result = commentService.postComment(100, 10, "好项目");

        // Assert - 评论仍然成功
        assertNotNull(result);
    }

    @Test
    @DisplayName("发表评论失败 - 内容为空")
    void postComment_EmptyContent() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> commentService.postComment(100, 10, ""));
        assertThrows(IllegalArgumentException.class,
                () -> commentService.postComment(100, 10, null));
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    // ==================== getCommentsByProjectId ====================

    @Test
    @DisplayName("获取项目评论列表成功 - 正常流程")
    void getCommentsByProjectId_Success() {
        // Arrange
        List<Comment> comments = List.of(testComment);
        when(commentMapper.selectByProjectId(10, 1)).thenReturn(comments);
        when(commentMapper.selectUserBatch(anyList())).thenReturn(List.of(testUserMap));

        // Act
        var result = commentService.getCommentsByProjectId(10, 1, 10, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(commentMapper).selectByProjectId(10, 1);
    }

    @Test
    @DisplayName("获取项目评论列表 - 空结果")
    void getCommentsByProjectId_Empty() {
        // Arrange
        when(commentMapper.selectByProjectId(10, 1)).thenReturn(List.of());

        // Act
        var result = commentService.getCommentsByProjectId(10, 1, 10, 1);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== deleteComment ====================

    @Test
    @DisplayName("删除评论成功 - 正常流程")
    void deleteComment_Success() {
        // Arrange
        when(commentMapper.selectById(1)).thenReturn(testComment);
        when(commentMapper.updateById(any(Comment.class))).thenReturn(1);

        // Act
        commentService.deleteComment(1, 100);

        // Assert
        assertEquals(0, testComment.getStatus()); // 软删除
        verify(commentMapper).selectById(1);
        verify(commentMapper).updateById(testComment);
    }

    @Test
    @DisplayName("删除评论失败 - 评论不存在")
    void deleteComment_NotFound() {
        // Arrange
        when(commentMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.deleteComment(99, 100));
        assertEquals(404, exception.getCode());
        assertEquals("评论不存在", exception.getMessage());
    }

    @Test
    @DisplayName("删除评论失败 - 无权限（非本人评论）")
    void deleteComment_NotOwner() {
        // Arrange
        when(commentMapper.selectById(1)).thenReturn(testComment);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.deleteComment(1, 999));
        assertEquals(403, exception.getCode());
        assertEquals("没有权限删除该评论", exception.getMessage());
    }

    // ==================== countByProjectId ====================

    @Test
    @DisplayName("统计项目评论数 - 正常流程")
    void countByProjectId_Success() {
        // Arrange
        when(commentMapper.countByProjectId(10)).thenReturn(5);

        // Act
        long result = commentService.countByProjectId(10);

        // Assert
        assertEquals(5L, result);
        verify(commentMapper).countByProjectId(10);
    }

    // ==================== getAllCommentsForAdmin ====================

    @Test
    @DisplayName("管理员获取所有评论成功 - 正常流程")
    void getAllCommentsForAdmin_Success() {
        // Arrange
        List<Comment> comments = List.of(testComment);
        when(commentMapper.selectAllWithPage(null)).thenReturn(comments);
        when(commentMapper.selectUserBatch(anyList())).thenReturn(List.of(testUserMap));
        when(commentMapper.getProjectNamesBatch(anyList())).thenReturn(
                List.of(Map.of("id", 10, "name", "测试项目")));

        // Act
        var result = commentService.getAllCommentsForAdmin(1, 10, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(commentMapper).selectAllWithPage(null);
    }

    @Test
    @DisplayName("管理员获取所有评论 - 空结果")
    void getAllCommentsForAdmin_Empty() {
        // Arrange
        when(commentMapper.selectAllWithPage(null)).thenReturn(List.of());

        // Act
        var result = commentService.getAllCommentsForAdmin(1, 10, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== searchCommentsForAdmin ====================

    @Test
    @DisplayName("管理员搜索评论成功 - 正常流程")
    void searchCommentsForAdmin_Success() {
        // Arrange
        when(commentMapper.searchByKeyword("测试")).thenReturn(List.of(testComment));
        when(commentMapper.selectUserBatch(anyList())).thenReturn(List.of(testUserMap));
        when(commentMapper.getProjectNamesBatch(anyList())).thenReturn(
                List.of(Map.of("id", 10, "name", "测试项目")));

        // Act
        var result = commentService.searchCommentsForAdmin("测试", 1, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(commentMapper).searchByKeyword("测试");
    }

    // ==================== adminDeleteComment ====================

    @Test
    @DisplayName("管理员物理删除评论成功 - 正常流程")
    void adminDeleteComment_Success() {
        // Arrange
        when(commentMapper.selectById(1)).thenReturn(testComment);
        when(commentMapper.forceDeleteById(1)).thenReturn(1);

        // Act
        commentService.adminDeleteComment(1);

        // Assert
        verify(commentMapper).selectById(1);
        verify(commentMapper).forceDeleteById(1);
    }

    @Test
    @DisplayName("管理员物理删除评论失败 - 评论不存在")
    void adminDeleteComment_NotFound() {
        // Arrange
        when(commentMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.adminDeleteComment(99));
        assertEquals(404, exception.getCode());
    }

    // ==================== countByStatus ====================

    @Test
    @DisplayName("统计评论数量（按状态）- 正常流程")
    void countByStatus_Success() {
        // Arrange
        when(commentMapper.countByStatus(1)).thenReturn(10);

        // Act
        long result = commentService.countByStatus(1);

        // Assert
        assertEquals(10L, result);
    }

    // ==================== countByKeywordForAdmin ====================

    @Test
    @DisplayName("统计评论数量（按关键词）- 正常流程")
    void countByKeywordForAdmin_Success() {
        // Arrange
        when(commentMapper.countByKeyword("测试")).thenReturn(3);

        // Act
        long result = commentService.countByKeywordForAdmin("测试");

        // Assert
        assertEquals(3L, result);
    }
}
