package com.bjutzxq.server.service;

import com.bjutzxq.common.BusinessException;
import com.bjutzxq.pojo.entity.Team;
import com.bjutzxq.server.mapper.TeamMapper;
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
@DisplayName("组队服务测试")
class TeamServiceTest {

    @Mock
    private TeamMapper teamMapper;

    @InjectMocks
    private TeamService teamService;

    private Team testTeam;
    private Map<String, Object> testUserMap;

    @BeforeEach
    void setUp() {
        testTeam = new Team();
        testTeam.setId(1);
        testTeam.setUserId(100);
        testTeam.setTitle("寻找前端伙伴");
        testTeam.setDescription("一起完成课设");
        testTeam.setCurrentMembers(1);
        testTeam.setNeededMembers(3);
        testTeam.setTag("PROJECT");
        testTeam.setCourseName("软件工程");
        testTeam.setStatus(1);

        testUserMap = new HashMap<>();
        testUserMap.put("id", 100);
        testUserMap.put("username", "测试用户");
        testUserMap.put("avatar", "https://example.com/avatar.png");
    }

    // ==================== createTeam ====================

    @Test
    @DisplayName("创建组队成功 - 正常流程")
    void createTeam_Success() {
        // Arrange
        when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            t.setId(1);
            return 1;
        });

        // Act
        Team result = teamService.createTeam(100, "寻找前端伙伴", "一起完成课设", 1, 3, "PROJECT", "软件工程");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("寻找前端伙伴", result.getTitle());
        assertEquals(100, result.getUserId());
        assertEquals(1, result.getStatus());
        verify(teamMapper).insert(any(Team.class));
    }

    @Test
    @DisplayName("创建组队 - tag为null默认PROJECT")
    void createTeam_DefaultTag() {
        // Arrange
        when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            t.setId(1);
            return 1;
        });

        // Act
        Team result = teamService.createTeam(100, "测试标题", "", 1, 3, null, null);

        // Assert
        assertEquals("PROJECT", result.getTag());
    }

    @Test
    @DisplayName("创建组队 - tag转大写")
    void createTeam_TagUpperCase() {
        // Arrange
        when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            t.setId(1);
            return 1;
        });

        // Act
        Team result = teamService.createTeam(100, "测试", "", 1, 3, "project", null);

        // Assert
        assertEquals("PROJECT", result.getTag());
    }

    @Test
    @DisplayName("创建组队失败 - 标题为空")
    void createTeam_EmptyTitle() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> teamService.createTeam(100, "", "", 1, 3, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> teamService.createTeam(100, null, "", 1, 3, null, null));
    }

    @Test
    @DisplayName("创建组队失败 - 需要人数小于2")
    void createTeam_InvalidNeededMembers() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> teamService.createTeam(100, "测试", "", 1, 1, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> teamService.createTeam(100, "测试", "", 1, null, null, null));
    }

    // ==================== getTeams ====================

    @Test
    @DisplayName("分页查询组队成功 - 正常流程")
    void getTeams_Success() {
        // Arrange
        List<Team> teams = List.of(testTeam);
        when(teamMapper.selectAll(null, null, null)).thenReturn(teams);
        when(teamMapper.selectUserBatch(anyList())).thenReturn(List.of(testUserMap));

        // Act
        var result = teamService.getTeams(1, 10, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(teamMapper).selectAll(null, null, null);
    }

    @Test
    @DisplayName("分页查询组队 - 空结果")
    void getTeams_Empty() {
        // Arrange
        when(teamMapper.selectAll(null, null, null)).thenReturn(List.of());

        // Act
        var result = teamService.getTeams(1, 10, null, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getUserTeams ====================

    @Test
    @DisplayName("查询用户组队成功 - 正常流程")
    void getUserTeams_Success() {
        // Arrange
        when(teamMapper.selectByUserId(100)).thenReturn(List.of(testTeam));
        when(teamMapper.selectUserBatch(anyList())).thenReturn(List.of(testUserMap));

        // Act
        var result = teamService.getUserTeams(100);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(teamMapper).selectByUserId(100);
    }

    // ==================== getTeamById ====================

    @Test
    @DisplayName("查询单个组队成功 - 正常流程")
    void getTeamById_Success() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(teamMapper.selectUserBatch(anyList())).thenReturn(List.of(testUserMap));

        // Act
        var result = teamService.getTeamById(1);

        // Assert
        assertNotNull(result);
        assertEquals("寻找前端伙伴", result.getTitle());
        verify(teamMapper).selectById(1);
    }

    @Test
    @DisplayName("查询单个组队 - 不存在返回null")
    void getTeamById_NotFound() {
        // Arrange
        when(teamMapper.selectById(99)).thenReturn(null);

        // Act
        var result = teamService.getTeamById(99);

        // Assert
        assertNull(result);
    }

    // ==================== updateTeam ====================

    @Test
    @DisplayName("更新组队成功 - 正常流程")
    void updateTeam_Success() {
        // Arrange
        Team update = new Team();
        update.setTitle("新标题");
        update.setDescription("新描述");
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(teamMapper.updateById(any(Team.class))).thenReturn(1);

        // Act
        teamService.updateTeam(1, 100, update);

        // Assert
        assertEquals("新标题", testTeam.getTitle());
        assertEquals("新描述", testTeam.getDescription());
        verify(teamMapper).selectById(1);
        verify(teamMapper).updateById(testTeam);
    }

    @Test
    @DisplayName("更新组队 - tag转大写")
    void updateTeam_TagUpperCase() {
        // Arrange
        Team update = new Team();
        update.setTag("internship");
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(teamMapper.updateById(any(Team.class))).thenReturn(1);

        // Act
        teamService.updateTeam(1, 100, update);

        // Assert
        assertEquals("INTERNSHIP", testTeam.getTag());
    }

    @Test
    @DisplayName("更新组队失败 - 组队不存在")
    void updateTeam_NotFound() {
        // Arrange
        when(teamMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamService.updateTeam(99, 100, new Team()));
        assertEquals(404, exception.getCode());
        assertEquals("组队不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新组队失败 - 非所有者修改")
    void updateTeam_NotOwner() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamService.updateTeam(1, 999, new Team()));
        assertEquals(403, exception.getCode());
        assertEquals("只能修改自己的组队", exception.getMessage());
    }

    // ==================== deleteTeam ====================

    @Test
    @DisplayName("删除组队成功 - 正常流程")
    void deleteTeam_Success() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(teamMapper.deleteById(1)).thenReturn(1);

        // Act
        teamService.deleteTeam(1, 100);

        // Assert
        verify(teamMapper).selectById(1);
        verify(teamMapper).deleteById(1);
    }

    @Test
    @DisplayName("删除组队失败 - 组队不存在")
    void deleteTeam_NotFound() {
        // Arrange
        when(teamMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamService.deleteTeam(99, 100));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("删除组队失败 - 非所有者删除")
    void deleteTeam_NotOwner() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamService.deleteTeam(1, 999));
        assertEquals(403, exception.getCode());
        assertEquals("只能删除自己的组队", exception.getMessage());
    }

    // ==================== adminDeleteTeam ====================

    @Test
    @DisplayName("管理员删除组队成功 - 正常流程")
    void adminDeleteTeam_Success() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(teamMapper.deleteById(1)).thenReturn(1);

        // Act
        teamService.adminDeleteTeam(1);

        // Assert
        verify(teamMapper).deleteById(1);
    }

    @Test
    @DisplayName("管理员删除组队失败 - 组队不存在")
    void adminDeleteTeam_NotFound() {
        // Arrange
        when(teamMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamService.adminDeleteTeam(99));
        assertEquals(404, exception.getCode());
    }

    // ==================== updateStatus ====================

    @Test
    @DisplayName("更新组队状态成功 - 正常流程")
    void updateStatus_Success() {
        // Arrange
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(teamMapper.updateById(any(Team.class))).thenReturn(1);

        // Act
        teamService.updateStatus(1, 2);

        // Assert
        assertEquals(2, testTeam.getStatus());
        verify(teamMapper).updateById(testTeam);
    }

    @Test
    @DisplayName("更新组队状态失败 - 组队不存在")
    void updateStatus_NotFound() {
        // Arrange
        when(teamMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamService.updateStatus(99, 2));
        assertEquals(404, exception.getCode());
    }

    // ==================== countAll ====================

    @Test
    @DisplayName("统计组队总数 - 正常流程")
    void countAll_Success() {
        // Arrange
        when(teamMapper.countAll(null, null, null)).thenReturn(25);

        // Act
        long result = teamService.countAll(null, null, null);

        // Assert
        assertEquals(25L, result);
        verify(teamMapper).countAll(null, null, null);
    }

    @Test
    @DisplayName("统计组队总数 - 按标签过滤")
    void countAll_WithTag() {
        // Arrange
        when(teamMapper.countAll("PROJECT", null, null)).thenReturn(10);

        // Act
        long result = teamService.countAll("PROJECT", null, null);

        // Assert
        assertEquals(10L, result);
        verify(teamMapper).countAll("PROJECT", null, null);
    }
}
