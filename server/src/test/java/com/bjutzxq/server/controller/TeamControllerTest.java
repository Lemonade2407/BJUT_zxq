package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.Team;
import com.bjutzxq.pojo.entity.TeamApplication;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.TeamApplicationService;
import com.bjutzxq.server.service.TeamService;
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
@DisplayName("组队控制器测试")
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @Mock
    private TeamApplicationService teamApplicationService;

    @InjectMocks
    private TeamController teamController;

    // ==================== getTeams ====================

    @Test
    @DisplayName("获取组队列表成功 - 正常流程")
    void getTeams_Success() {
        // Arrange
        when(teamService.getTeams(anyInt(), anyInt(), any(), any(), any())).thenReturn(List.of());
        when(teamService.countAll(any(), any(), any())).thenReturn(0L);

        // Act
        var result = teamController.getTeams(1, 10, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== getTeam ====================

    @Test
    @DisplayName("获取单个组队失败 - 不存在返回500")
    void getTeam_NotFound() {
        // Arrange
        when(teamService.getTeamById(99)).thenReturn(null);

        // Act
        var result = teamController.getTeam(99);

        // Assert
        assertNotNull(result);
        assertEquals(500, result.getCode());
    }

    // ==================== createTeam ====================

    @Test
    @DisplayName("创建组队成功 - 正常流程")
    void createTeam_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            Map<String, Object> body = Map.of(
                    "title", "找队友",
                    "description", "一起做项目",
                    "currentMembers", 1,
                    "neededMembers", 3,
                    "tag", "PROJECT",
                    "courseName", "软件工程");
            Team team = new Team();
            team.setId(1);
            team.setTitle("找队友");
            when(teamService.createTeam(anyInt(), anyString(), anyString(), anyInt(),
                    anyInt(), anyString(), anyString())).thenReturn(team);

            // Act
            Result<Team> result = teamController.createTeam(body);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    // ==================== deleteTeam ====================

    @Test
    @DisplayName("删除组队成功 - 正常流程")
    void deleteTeam_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);

            // Act
            Result<Void> result = teamController.deleteTeam(1);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            verify(teamService).deleteTeam(1, 100);
        }
    }

    // ==================== getMyTeams ====================

    @Test
    @DisplayName("获取我的组队 - 正常流程")
    void getMyTeams_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(teamService.getUserTeams(100)).thenReturn(List.of());

            // Act
            var result = teamController.getMyTeams();

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    // ==================== apply ====================

    @Test
    @DisplayName("申请组队成功 - 正常流程")
    void apply_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(200);
            Map<String, String> body = Map.of("message", "我想加入");
            TeamApplication app = new TeamApplication();
            app.setId(1);
            when(teamApplicationService.apply(1, 200, "我想加入")).thenReturn(app);

            // Act
            Result<TeamApplication> result = teamController.apply(1, body);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    // ==================== approveApplication ====================

    @Test
    @DisplayName("审批通过申请 - 正常流程")
    void approveApplication_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            doNothing().when(teamApplicationService).approve(1, 100);

            // Act
            Result<Void> result = teamController.approveApplication(1);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    // ==================== hasApplied ====================

    @Test
    @DisplayName("检查是否已申请 - 正常流程")
    void hasApplied_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(200);
            when(teamApplicationService.hasApplied(1, 200)).thenReturn(true);

            // Act
            Result<Boolean> result = teamController.hasApplied(1);

            // Assert
            assertNotNull(result);
            assertTrue(result.getData());
        }
    }
}
