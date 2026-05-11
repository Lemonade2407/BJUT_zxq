package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.dto.PageResult;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.pojo.vo.ProjectVO;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("项目控制器测试")
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectController projectController;

    private Project testProject;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId(1);
        testProject.setName("测试项目");
        testProject.setDescription("项目描述");
        testProject.setOwnerId(100);
        testProject.setVisibility(1);
        testProject.setProjectType("课设项目");
    }

    // ==================== selectById ====================

    @Test
    @DisplayName("查询项目成功 - 正常流程")
    void selectById_Success() {
        // Arrange
        when(projectService.selectById(1)).thenReturn(testProject);
        doNothing().when(projectService).incrementViewCount(1);
        when(projectService.enrichProject(any(Project.class), isNull())).thenReturn(testProject);

        // Act
        Result<Project> result = projectController.selectById(1);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("测试项目", result.getData().getName());
        verify(projectService).incrementViewCount(1);
        verify(projectService).enrichProject(any(Project.class), isNull());
    }

    @Test
    @DisplayName("查询项目失败 - 项目不存在")
    void selectById_NotFound() {
        // Arrange
        when(projectService.selectById(99)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectController.selectById(99));
        assertTrue(exception.getMessage().contains("项目不存在"));
    }

    // ==================== selectByName ====================

    @Test
    @DisplayName("按名称搜索项目 - 正常流程")
    void selectByName_Success() {
        // Arrange
        when(projectService.selectByName("测试", 1, 10)).thenReturn(List.of(testProject));
        when(projectService.countByName("测试")).thenReturn(1L);
        when(projectService.enrichProjects(anyList(), isNull())).thenReturn(List.of(testProject));

        // Act
        Result<PageResult<Project>> result = projectController.selectByName("测试", 1, 10);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== selectMyProjects ====================

    @Test
    @DisplayName("查询我的项目 - 正常流程")
    void selectMyProjects_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(projectService.selectByUserId(100, 1, 10)).thenReturn(List.of(testProject));
            when(projectService.countByUserId(100)).thenReturn(1L);
            when(projectService.enrichProjects(anyList(), eq(100))).thenReturn(List.of(testProject));

            // Act
            Result<PageResult<Project>> result = projectController.selectMyProjects(1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    // ==================== getPublicProjects ====================

    @Test
    @DisplayName("获取公开项目列表 - 正常流程")
    void getPublicProjects_Success() {
        // Arrange
        when(projectService.getPublicProjects(1, 10)).thenReturn(List.of(testProject));
        when(projectService.countPublicProjects()).thenReturn(1L);
        when(projectService.enrichProjects(anyList(), isNull())).thenReturn(List.of(testProject));

        // Act
        Result<PageResult<Project>> result = projectController.getPublicProjects(1, 10);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== getTrendingProjects ====================

    @Test
    @DisplayName("获取热门项目 - 正常流程")
    void getTrendingProjects_Success() {
        // Arrange
        when(projectService.getTrendingProjects(10)).thenReturn(List.of(testProject));

        // Act
        Result<List<Project>> result = projectController.getTrendingProjects(10);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("获取热门项目 - limit超出50截断为50")
    void getTrendingProjects_LimitExceedsMax() {
        // Arrange
        when(projectService.getTrendingProjects(50)).thenReturn(List.of());

        // Act
        Result<List<Project>> result = projectController.getTrendingProjects(100);

        // Assert
        assertNotNull(result);
        verify(projectService).getTrendingProjects(50);
    }

    // ==================== getProjectTypes ====================

    @Test
    @DisplayName("获取项目类型列表 - 正常流程")
    void getProjectTypes_Success() {
        // Act
        Result<List<Map<String, Object>>> result = projectController.getProjectTypes();

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().size() > 0);
    }

    // ==================== deleteProject ====================

    @Test
    @DisplayName("删除项目 - 正常流程")
    void deleteProject_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            when(projectService.deleteProject(1, 100)).thenReturn(true);

            // Act
            Result<Boolean> result = projectController.deleteProject(1);

            // Assert
            assertNotNull(result);
            assertTrue(result.getData());
            verify(projectService).deleteProject(1, 100);
        }
    }

    // ==================== filterProjects ====================

    @Test
    @DisplayName("筛选项目 - 正常流程")
    void filterProjects_Success() {
        // Arrange
        when(projectService.selectByClassAndCourse(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(testProject));
        when(projectService.countByClassAndCourse(any(), any(), any())).thenReturn(1L);
        when(projectService.enrichProjects(anyList(), isNull())).thenReturn(List.of(testProject));

        // Act
        Result<PageResult<ProjectVO>> result = projectController.filterProjects(
                "软件2101", "软件工程", "课设项目", 1, 10);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== createProject ====================

    @Test
    @DisplayName("创建项目成功 - 正常流程")
    void createProject_Success() {
        // Arrange
        try (MockedStatic<UserIdContext> ctxMock = mockStatic(UserIdContext.class)) {
            ctxMock.when(UserIdContext::getCurrentUserId).thenReturn(100);
            var dto = new com.bjutzxq.pojo.dto.ProjectDTO();
            dto.setName("新项目");
            dto.setDescription("描述");
            dto.setVisibility(1);
            dto.setProjectType("课设项目");
            dto.setTagIds(List.of(1, 2));
            when(projectService.createProject(any(Project.class), anyList())).thenReturn(testProject);
            when(projectService.enrichProject(any(Project.class), eq(100))).thenReturn(testProject);

            // Act
            Result<ProjectVO> result = projectController.createProject(dto);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            verify(projectService).createProject(any(Project.class), eq(List.of(1, 2)));
        }
    }
}
