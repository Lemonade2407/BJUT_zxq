package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.Comment;
import com.bjutzxq.pojo.vo.CommentVO;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("评论控制器测试")
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    // ==================== postComment ====================

    @Test
    @DisplayName("发表评论成功 - 正常流程")
    void postComment_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            Comment comment = new Comment();
            comment.setId(1);
            comment.setContent("好项目");
            when(commentService.postComment(100, 10, "好项目")).thenReturn(comment);
            CommentVO mockVO = new CommentVO();
            mockVO.setId(1);
            when(commentService.getCommentsByProjectId(10, 1, 10, 1))
                    .thenReturn(List.of(mockVO));

            // Act
            Result<CommentVO> result = commentController.postComment(10, "好项目");

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            verify(commentService).postComment(100, 10, "好项目");
        }
    }

    // ==================== getComments ====================

    @Test
    @DisplayName("获取评论列表成功 - 正常流程")
    void getComments_Success() {
        // Arrange
        when(commentService.getCommentsByProjectId(10, 1, 10, 1))
                .thenReturn(List.of(new CommentVO()));
        when(commentService.countByProjectId(10)).thenReturn(1L);

        // Act
        var result = commentController.getComments(10, 1, 10, 1);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== deleteComment ====================

    @Test
    @DisplayName("删除评论成功 - 正常流程")
    void deleteComment_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);

            // Act
            Result<Boolean> result = commentController.deleteComment(10, 1);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertTrue(result.getData());
            verify(commentService).deleteComment(1, 100);
        }
    }
}
