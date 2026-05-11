package com.bjutzxq.server.service;

import com.bjutzxq.common.BusinessException;
import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.entity.Team;
import com.bjutzxq.pojo.entity.TeamApplication;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.mapper.TeamApplicationMapper;
import com.bjutzxq.server.mapper.TeamMapper;
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
@DisplayName("组队申请服务测试")
class TeamApplicationServiceTest {

    @Mock
    private TeamApplicationMapper appMapper;

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TeamApplicationService teamApplicationService;

    private Team testTeam;
    private User testApplicant;
    private User teamCreator;
    private TeamApplication testApp;

    @BeforeEach
    void setUp() {
        teamCreator = new User();
        teamCreator.setId(100);
        teamCreator.setUsername("组长用户");

        testTeam = new Team();
        testTeam.setId(1);
        testTeam.setUserId(100); // 组长ID
        testTeam.setTitle("寻找前端伙伴");
        testTeam.setCurrentMembers(2);
        testTeam.setNeededMembers(3);
        testTeam.setStatus(1);

        testApplicant = new User();
        testApplicant.setId(200);
        testApplicant.setUsername("申请者");

        testApp = new TeamApplication();
        testApp.setId(1);
        testApp.setTeamId(1);
        testApp.setApplicantId(200);
        testApp.setMessage("我想加入");
        testApp.setStatus(0); // 待审批
    }

    // ==================== apply ====================

    @Test
    @DisplayName("申请组队成功 - 正常流程")
    void apply_Success() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(appMapper.selectByTeamAndUser(1, 200)).thenReturn(null);
        when(appMapper.insert(any(TeamApplication.class))).thenAnswer(invocation -> {
            TeamApplication a = invocation.getArgument(0);
            a.setId(1);
            return 1;
        });
        when(userMapper.selectById(200)).thenReturn(testApplicant);

        // Act
        TeamApplication result = teamApplicationService.apply(1, 200, "我想加入");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(1, result.getTeamId());
        assertEquals(200, result.getApplicantId());
        assertEquals(0, result.getStatus());
        verify(appMapper).insert(any(TeamApplication.class));
    }

    @Test
    @DisplayName("申请组队 - 发送通知给组长")
    void apply_SendsNotification() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(appMapper.selectByTeamAndUser(1, 200)).thenReturn(null);
        when(appMapper.insert(any(TeamApplication.class))).thenReturn(1);
        when(userMapper.selectById(200)).thenReturn(testApplicant);

        // Act
        teamApplicationService.apply(1, 200, "我想加入");

        // Assert
        verify(notificationService).createNotification(
                eq(100), eq(200), isNull(),
                eq(NotificationType.TEAM_APPLICATION), anyString());
    }

    @Test
    @DisplayName("申请组队失败 - 组队不存在")
    void apply_TeamNotFound() {
        // Arrange
        when(teamMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamApplicationService.apply(99, 200, ""));
        assertEquals(404, exception.getCode());
        assertEquals("组队不存在", exception.getMessage());
    }

    @Test
    @DisplayName("申请组队失败 - 不能申请自己的组队")
    void apply_OwnTeam() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamApplicationService.apply(1, 100, ""));
        assertEquals(400, exception.getCode());
        assertEquals("不能申请自己的组队", exception.getMessage());
    }

    @Test
    @DisplayName("申请组队失败 - 重复申请")
    void apply_Duplicate() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(appMapper.selectByTeamAndUser(1, 200)).thenReturn(testApp);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamApplicationService.apply(1, 200, ""));
        assertEquals(409, exception.getCode());
        assertEquals("您已经申请过该组队", exception.getMessage());
    }

    // ==================== getTeamApplications ====================

    @Test
    @DisplayName("获取组队申请列表成功 - 正常流程")
    void getTeamApplications_Success() {
        // Arrange
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", 200);
        userMap.put("username", "申请者");
        userMap.put("avatar", "https://example.com/avatar.png");
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(appMapper.selectByTeamId(1)).thenReturn(List.of(testApp));
        when(appMapper.selectUserBatch(anyList())).thenReturn(List.of(userMap));

        // Act
        var result = teamApplicationService.getTeamApplications(1, 100);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appMapper).selectByTeamId(1);
    }

    @Test
    @DisplayName("获取组队申请列表失败 - 组队不存在")
    void getTeamApplications_TeamNotFound() {
        // Arrange
        when(teamMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamApplicationService.getTeamApplications(99, 100));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("获取组队申请列表失败 - 非组长查看")
    void getTeamApplications_NotOwner() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamApplicationService.getTeamApplications(1, 999));
        assertEquals(403, exception.getCode());
        assertEquals("只有组长可以查看申请列表", exception.getMessage());
    }

    // ==================== getMyApplications ====================

    @Test
    @DisplayName("获取我的申请列表 - 正常流程")
    void getMyApplications_Success() {
        // Arrange
        when(appMapper.selectByApplicantId(200)).thenReturn(List.of(testApp));
        when(appMapper.selectUserBatch(anyList())).thenReturn(List.of());

        // Act
        var result = teamApplicationService.getMyApplications(200);

        // Assert
        assertNotNull(result);
        verify(appMapper).selectByApplicantId(200);
    }

    // ==================== approve ====================

    @Test
    @DisplayName("审批通过 - 正常流程")
    void approve_Success() {
        // Arrange
        when(appMapper.selectById(1)).thenReturn(testApp);
        when(appMapper.getTeamCreatorId(1)).thenReturn(100);
        when(appMapper.updateStatus(1, 1)).thenReturn(1);
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(teamMapper.updateById(any(Team.class))).thenReturn(1);

        // Act
        teamApplicationService.approve(1, 100);

        // Assert
        verify(appMapper).updateStatus(1, 1);
        assertEquals(3, testTeam.getCurrentMembers()); // 成员数+1
        verify(teamMapper).updateById(testTeam);
    }

    @Test
    @DisplayName("审批通过 - 满员时更新状态为2")
    void approve_TeamFull() {
        // Arrange
        testTeam.setCurrentMembers(2);
        testTeam.setNeededMembers(3); // 通过后currentMembers=3, 满员
        when(appMapper.selectById(1)).thenReturn(testApp);
        when(appMapper.getTeamCreatorId(1)).thenReturn(100);
        when(appMapper.updateStatus(1, 1)).thenReturn(1);
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(teamMapper.updateById(any(Team.class))).thenReturn(1);

        // Act
        teamApplicationService.approve(1, 100);

        // Assert
        assertEquals(2, testTeam.getStatus()); // 满员状态
    }

    @Test
    @DisplayName("审批通过 - 发送通知给申请者")
    void approve_SendsNotification() {
        // Arrange
        when(appMapper.selectById(1)).thenReturn(testApp);
        when(appMapper.getTeamCreatorId(1)).thenReturn(100);
        when(appMapper.updateStatus(1, 1)).thenReturn(1);
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(teamMapper.updateById(any(Team.class))).thenReturn(1);

        // Act
        teamApplicationService.approve(1, 100);

        // Assert
        verify(notificationService).createNotification(
                eq(200), eq(100), isNull(),
                eq(NotificationType.TEAM_APPLICATION), anyString());
    }

    @Test
    @DisplayName("审批失败 - 申请不存在")
    void approve_AppNotFound() {
        // Arrange
        when(appMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamApplicationService.approve(99, 100));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("审批失败 - 非组长审核")
    void approve_NotCreator() {
        // Arrange
        when(appMapper.selectById(1)).thenReturn(testApp);
        when(appMapper.getTeamCreatorId(1)).thenReturn(100);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamApplicationService.approve(1, 999));
        assertEquals(403, exception.getCode());
    }

    @Test
    @DisplayName("审批失败 - 申请已处理")
    void approve_AlreadyProcessed() {
        // Arrange
        testApp.setStatus(1); // 已通过
        when(appMapper.selectById(1)).thenReturn(testApp);
        when(appMapper.getTeamCreatorId(1)).thenReturn(100);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamApplicationService.approve(1, 100));
        assertEquals(409, exception.getCode());
        assertEquals("该申请已处理", exception.getMessage());
    }

    // ==================== reject ====================

    @Test
    @DisplayName("审批拒绝 - 正常流程")
    void reject_Success() {
        // Arrange
        when(appMapper.selectById(1)).thenReturn(testApp);
        when(appMapper.getTeamCreatorId(1)).thenReturn(100);
        when(appMapper.updateStatus(1, 2)).thenReturn(1);
        when(teamMapper.selectById(1)).thenReturn(testTeam);

        // Act
        teamApplicationService.reject(1, 100);

        // Assert
        verify(appMapper).updateStatus(1, 2);
        verify(notificationService).createNotification(
                eq(200), eq(100), isNull(),
                eq(NotificationType.TEAM_APPLICATION), anyString());
    }

    @Test
    @DisplayName("审批拒绝失败 - 非组长审核")
    void reject_NotCreator() {
        // Arrange
        when(appMapper.selectById(1)).thenReturn(testApp);
        when(appMapper.getTeamCreatorId(1)).thenReturn(100);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamApplicationService.reject(1, 999));
        assertEquals(403, exception.getCode());
    }

    // ==================== hasApplied ====================

    @Test
    @DisplayName("检查是否已申请 - 已申请返回true")
    void hasApplied_True() {
        // Arrange
        when(appMapper.selectByTeamAndUser(1, 200)).thenReturn(testApp);

        // Act
        boolean result = teamApplicationService.hasApplied(1, 200);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("检查是否已申请 - 未申请返回false")
    void hasApplied_False() {
        // Arrange
        when(appMapper.selectByTeamAndUser(1, 200)).thenReturn(null);

        // Act
        boolean result = teamApplicationService.hasApplied(1, 200);

        // Assert
        assertFalse(result);
    }
}
