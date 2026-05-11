package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.common.Role;
import com.bjutzxq.pojo.dto.PageResult;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.service.*;
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
@DisplayName("管理员控制器测试")
class AdminControllerTest {

    @Mock private UserService userService;
    @Mock private ProjectService projectService;
    @Mock private StatisticsService statisticsService;
    @Mock private CommentService commentService;
    @Mock private TeamService teamService;

    @InjectMocks
    private AdminController adminController;

    // ==================== getAllUsers ====================

    @Test
    @DisplayName("获取所有用户 - 正常流程")
    void getAllUsers_Success() {
        // Arrange
        when(userService.queryUsers(null, null, 1, 20)).thenReturn(List.of());
        when(userService.countAllUsers()).thenReturn(0L);

        // Act
        Result<PageResult<User>> result = adminController.getAllUsers(1, 20);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== searchUsers ====================

    @Test
    @DisplayName("搜索用户 - 正常流程")
    void searchUsers_Success() {
        // Arrange
        when(userService.queryUsers(null, "test", 1, 20)).thenReturn(List.of());
        when(userService.countUsersByKeyword("test")).thenReturn(0L);

        // Act
        Result<PageResult<User>> result = adminController.searchUsers("test", 1, 20);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== getStudents ====================

    @Test
    @DisplayName("获取学生列表 - 正常流程")
    void getStudents_Success() {
        // Arrange
        when(userService.queryUsers(Role.USER, null, 1, 20)).thenReturn(List.of());

        // Act
        Result<List<User>> result = adminController.getStudents(1, 20);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== banUser ====================

    @Test
    @DisplayName("封禁用户 - 正常流程")
    void banUser_Success() {
        // Arrange
        doNothing().when(userService).banUser(1);

        // Act
        Result<Void> result = adminController.banUser(1);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(userService).banUser(1);
    }

    // ==================== unbanUser ====================

    @Test
    @DisplayName("解封用户 - 正常流程")
    void unbanUser_Success() {
        // Arrange
        doNothing().when(userService).unbanUser(1);

        // Act
        Result<Void> result = adminController.unbanUser(1);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== setUserRole ====================

    @Test
    @DisplayName("设置用户角色 - 正常流程")
    void setUserRole_Success() {
        // Arrange
        doNothing().when(userService).setUserRole(eq(1), any(Role.class));

        // Act
        Result<Void> result = adminController.setUserRole(1, 1);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(userService).setUserRole(eq(1), any(Role.class));
    }

    // ==================== deleteUser ====================

    @Test
    @DisplayName("删除用户 - 正常流程")
    void deleteUser_Success() {
        // Arrange
        doNothing().when(userService).deleteUser(1);

        // Act
        Result<Void> result = adminController.deleteUser(1);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== getAllComments ====================

    @Test
    @DisplayName("获取所有评论 - 正常流程")
    void getAllComments_Success() {
        // Arrange
        when(commentService.getAllCommentsForAdmin(1, 20, null)).thenReturn(List.of());
        when(commentService.countByStatus(null)).thenReturn(0L);

        // Act
        var result = adminController.getAllComments(1, 20, null);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== deleteComment ====================

    @Test
    @DisplayName("删除评论 - 正常流程")
    void deleteComment_Success() {
        // Arrange
        doNothing().when(commentService).adminDeleteComment(1);

        // Act
        Result<Void> result = adminController.deleteComment(1);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== getAllTeams ====================

    @Test
    @DisplayName("获取所有组队 - 正常流程")
    void getAllTeams_Success() {
        // Arrange
        when(teamService.getTeams(anyInt(), anyInt(), any(), any(), isNull())).thenReturn(List.of());
        when(teamService.countAll(any(), any(), any())).thenReturn(0L);

        // Act
        var result = adminController.getAllTeams(1, 10, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== deleteTeam ====================

    @Test
    @DisplayName("删除组队 - 正常流程")
    void deleteTeam_Success() {
        // Arrange
        doNothing().when(teamService).adminDeleteTeam(1);

        // Act
        Result<Void> result = adminController.deleteTeam(1);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== getStatistics ====================

    @Test
    @DisplayName("获取统计信息 - 正常流程")
    void getStatistics_Success() {
        // Arrange
        when(statisticsService.getAdminStatistics()).thenReturn(Map.of("cards", Map.of()));

        // Act
        Result<Map<String, Object>> result = adminController.getStatistics();

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== updateUser ====================

    @Test
    @DisplayName("更新用户信息 - 正常流程")
    void updateUser_Success() {
        // Arrange
        Map<String, Object> userInfo = Map.of("username", "新用户名", "role", "ADMIN");
        doNothing().when(userService).updateUserByAdmin(anyInt(), any(), any(), any(), any(), any(),
                any(), any(), any(), any());

        // Act
        Result<Void> result = adminController.updateUser(1, userInfo);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(userService).updateUserByAdmin(anyInt(), any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    // ==================== updateProject ====================

    @Test
    @DisplayName("更新项目信息 - 正常流程")
    void updateProject_Success() {
        // Arrange
        Map<String, Object> projectInfo = Map.of("name", "新项目名");
        var existingProject = new com.bjutzxq.pojo.entity.Project();
        existingProject.setId(1);
        existingProject.setName("旧项目名");
        existingProject.setOwnerId(100);
        when(projectService.selectById(1)).thenReturn(existingProject);
        when(projectService.updateProject(any(), isNull())).thenReturn(existingProject);

        // Act
        Result<Void> result = adminController.updateProject(1, projectInfo);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }
}
