package com.bjutzxq.server.service;

import com.bjutzxq.pojo.entity.*;
import com.bjutzxq.server.mapper.*;
import com.bjutzxq.server.util.OssUtil;
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
@DisplayName("项目服务测试")
class ProjectServiceTest {

    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectTagService projectTagService;
    @Mock private UserMapper userMapper;
    @Mock private StarMapper starMapper;
    @Mock private WatchMapper watchMapper;
    @Mock private ProjectFileMapper projectFileMapper;
    @Mock private OssUtil ossUtil;
    @Mock private ProjectFileService projectFileService;

    @InjectMocks
    private ProjectService projectService;

    private Project testProject;
    private User testOwner;
    private Tag testTag;

    @BeforeEach
    void setUp() {
        testOwner = new User();
        testOwner.setId(100);
        testOwner.setUsername("项目作者");
        testOwner.setClassName("软件工程2101");

        testProject = new Project();
        testProject.setId(1);
        testProject.setName("测试项目");
        testProject.setDescription("项目描述");
        testProject.setOwnerId(100);
        testProject.setVisibility(1);
        testProject.setProjectType("课设项目");
        testProject.setCourseName("软件工程");
        testProject.setStarCount(10);
        testProject.setWatchCount(5);

        testTag = new Tag();
        testTag.setId(1);
        testTag.setName("Java");
    }

    // ==================== createProject ====================

    @Test
    @DisplayName("创建项目成功 - 正常流程")
    void createProject_Success() {
        // Arrange
        when(projectMapper.selectByUserId(100)).thenReturn(List.of());
        when(projectMapper.insert(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(1);
            return 1;
        });
        doNothing().when(projectTagService).setProjectTags(anyInt(), anyList());

        // Act
        Project result = projectService.createProject(testProject, List.of(1));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("测试项目", result.getName());
        verify(projectMapper).selectByUserId(100);
        verify(projectMapper).insert(testProject);
        verify(projectTagService).setProjectTags(1, List.of(1));
    }

    @Test
    @DisplayName("创建项目 - null标签不设置")
    void createProject_NullTags() {
        // Arrange
        when(projectMapper.selectByUserId(100)).thenReturn(List.of());
        when(projectMapper.insert(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(1);
            return 1;
        });

        // Act
        Project result = projectService.createProject(testProject, null);

        // Assert
        assertNotNull(result);
        verify(projectTagService, never()).setProjectTags(anyInt(), anyList());
    }

    @Test
    @DisplayName("创建项目失败 - 用户已创建同名项目")
    void createProject_DuplicateName() {
        // Arrange
        Project existing = new Project();
        existing.setId(2);
        existing.setName("测试项目");
        existing.setOwnerId(100);
        when(projectMapper.selectByUserId(100)).thenReturn(List.of(existing));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectService.createProject(testProject, null));
        assertEquals("您已创建了同名项目", exception.getMessage());
        verify(projectMapper, never()).insert(any(Project.class));
    }

    // ==================== updateProject ====================

    @Test
    @DisplayName("更新项目成功 - 正常流程")
    void updateProject_Success() {
        // Arrange
        Project update = new Project();
        update.setId(1);
        update.setOwnerId(100);
        update.setName("更新后的项目名");
        update.setDescription("新描述");
        when(projectMapper.selectById(1)).thenReturn(testProject);
        when(projectMapper.selectByUserId(100)).thenReturn(List.of(testProject));
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        // Act
        Project result = projectService.updateProject(update, null);

        // Assert
        assertNotNull(result);
        assertEquals("更新后的项目名", result.getName());
        assertEquals("新描述", result.getDescription());
        verify(projectMapper).selectById(1);
        verify(projectMapper).updateById(any(Project.class));
    }

    @Test
    @DisplayName("更新项目 - 含标签更新")
    void updateProject_WithTags() {
        // Arrange
        Project update = new Project();
        update.setId(1);
        update.setOwnerId(100);
        update.setName("新名称");
        when(projectMapper.selectById(1)).thenReturn(testProject);
        when(projectMapper.selectByUserId(100)).thenReturn(List.of(testProject));
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        // Act
        Project result = projectService.updateProject(update, List.of(1, 2));

        // Assert
        assertNotNull(result);
        verify(projectTagService).setProjectTags(1, List.of(1, 2));
    }

    @Test
    @DisplayName("更新项目失败 - 项目信息为null")
    void updateProject_NullProject() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> projectService.updateProject(null, null));
    }

    @Test
    @DisplayName("更新项目失败 - 项目ID为空")
    void updateProject_NullId() {
        // Act & Assert
        Project update = new Project();
        assertThrows(IllegalArgumentException.class,
                () -> projectService.updateProject(update, null));
    }

    @Test
    @DisplayName("更新项目失败 - 项目不存在")
    void updateProject_NotFound() {
        // Arrange
        Project update = new Project();
        update.setId(99);
        when(projectMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectService.updateProject(update, null));
        assertEquals("项目不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新项目失败 - 无权限修改")
    void updateProject_NotOwner() {
        // Arrange
        Project update = new Project();
        update.setId(1);
        update.setOwnerId(999); // 不同用户
        when(projectMapper.selectById(1)).thenReturn(testProject);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectService.updateProject(update, null));
        assertEquals("无权限修改该项目", exception.getMessage());
    }

    @Test
    @DisplayName("更新项目失败 - 新名称与已有项目重名")
    void updateProject_DuplicateName() {
        // Arrange
        Project otherProject = new Project();
        otherProject.setId(2);
        otherProject.setName("重复项目名");
        otherProject.setOwnerId(100);

        Project update = new Project();
        update.setId(1);
        update.setOwnerId(100);
        update.setName("重复项目名");
        when(projectMapper.selectById(1)).thenReturn(testProject);
        when(projectMapper.selectByUserId(100)).thenReturn(List.of(testProject, otherProject));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectService.updateProject(update, null));
        assertEquals("您已创建了同名项目", exception.getMessage());
    }

    // ==================== deleteProject ====================

    @Test
    @DisplayName("删除项目成功 - 级联清理")
    void deleteProject_Success() {
        // Arrange
        when(projectMapper.selectById(1)).thenReturn(testProject);
        when(projectFileService.deleteAllProjectFiles(1)).thenReturn(3);
        doNothing().when(projectTagService).removeProjectTags(1);
        when(starMapper.deleteByProjectId(1)).thenReturn(5);
        when(watchMapper.deleteByProjectId(1)).thenReturn(2);
        when(projectMapper.deleteById(1)).thenReturn(1);

        // Act
        boolean result = projectService.deleteProject(1, 100);

        // Assert
        assertTrue(result);
        verify(projectFileService).deleteAllProjectFiles(1);
        verify(projectTagService).removeProjectTags(1);
        verify(starMapper).deleteByProjectId(1);
        verify(watchMapper).deleteByProjectId(1);
        verify(projectMapper).deleteById(1);
    }

    @Test
    @DisplayName("删除项目 - 文件删除失败不影响项目删除")
    void deleteProject_FileCleanupFails() {
        // Arrange
        when(projectMapper.selectById(1)).thenReturn(testProject);
        when(projectFileService.deleteAllProjectFiles(1))
                .thenThrow(new RuntimeException("OSS删除失败"));
        doNothing().when(projectTagService).removeProjectTags(1);
        when(starMapper.deleteByProjectId(1)).thenReturn(0);
        when(watchMapper.deleteByProjectId(1)).thenReturn(0);
        when(projectMapper.deleteById(1)).thenReturn(1);

        // Act - 不应抛出异常
        boolean result = projectService.deleteProject(1, 100);

        // Assert
        assertTrue(result);
        verify(projectMapper).deleteById(1);
    }

    @Test
    @DisplayName("删除项目失败 - 项目不存在")
    void deleteProject_NotFound() {
        // Arrange
        when(projectMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectService.deleteProject(99, 100));
        assertEquals("项目不存在", exception.getMessage());
    }

    @Test
    @DisplayName("删除项目失败 - 无权限")
    void deleteProject_NotOwner() {
        // Arrange
        when(projectMapper.selectById(1)).thenReturn(testProject);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectService.deleteProject(1, 999));
        assertEquals("无权限删除该项目", exception.getMessage());
    }

    // ==================== selectById ====================

    @Test
    @DisplayName("查询项目成功 - 正常流程")
    void selectById_Success() {
        // Arrange
        when(projectMapper.selectById(1)).thenReturn(testProject);

        // Act
        Project result = projectService.selectById(1);

        // Assert
        assertNotNull(result);
        assertEquals("测试项目", result.getName());
    }

    @Test
    @DisplayName("查询项目 - 不存在返回null")
    void selectById_NotFound() {
        // Arrange
        when(projectMapper.selectById(99)).thenReturn(null);

        // Act
        Project result = projectService.selectById(99);

        // Assert
        assertNull(result);
    }

    // ==================== enrichProject ====================

    @Test
    @DisplayName("丰富项目信息 - 含用户交互状态")
    void enrichProject_WithUser() {
        // Arrange
        when(projectTagService.getProjectTags(1)).thenReturn(List.of(testTag));
        when(userMapper.selectById(100)).thenReturn(testOwner);
        Star star = new Star();
        star.setId(1);
        when(starMapper.selectByUserIdAndProjectId(200, 1)).thenReturn(star);
        when(watchMapper.selectByUserIdAndProjectId(200, 1)).thenReturn(null);

        // Act
        Project result = projectService.enrichProject(testProject, 200);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTags().size());
        assertEquals("项目作者", result.getAuthor());
        assertTrue(result.getIsStarred());
        assertFalse(result.getIsWatched());
    }

    @Test
    @DisplayName("丰富项目信息 - 未登录用户")
    void enrichProject_NoUser() {
        // Arrange
        when(projectTagService.getProjectTags(1)).thenReturn(List.of());
        when(userMapper.selectById(100)).thenReturn(testOwner);

        // Act
        Project result = projectService.enrichProject(testProject, null);

        // Assert
        assertNotNull(result);
        assertFalse(result.getIsStarred());
        assertFalse(result.getIsWatched());
    }

    @Test
    @DisplayName("丰富项目信息 - null项目返回null")
    void enrichProject_NullProject() {
        // Act
        Project result = projectService.enrichProject(null, 200);

        // Assert
        assertNull(result);
    }

    // ==================== enrichProjects ====================

    @Test
    @DisplayName("批量丰富项目信息 - 正常流程")
    void enrichProjects_Success() {
        // Arrange
        List<Project> projects = List.of(testProject);
        when(projectTagService.getProjectTagsBatch(List.of(1))).thenReturn(Map.of(1, List.of(testTag)));
        when(userMapper.selectBatchIds(List.of(100))).thenReturn(List.of(testOwner));
        when(starMapper.selectStarredProjectIds(eq(200), anyList())).thenReturn(List.of());
        when(watchMapper.selectWatchedProjectIds(eq(200), anyList())).thenReturn(List.of());

        // Act
        List<Project> result = projectService.enrichProjects(projects, 200);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("项目作者", result.get(0).getAuthor());
        assertEquals("软件工程2101", result.get(0).getOwnerClassName());
        assertEquals(1, result.get(0).getTags().size());
    }

    @Test
    @DisplayName("批量丰富项目信息 - null或空列表直接返回")
    void enrichProjects_NullOrEmpty() {
        // Act
        List<Project> nullResult = projectService.enrichProjects(null, 200);
        List<Project> emptyResult = projectService.enrichProjects(List.of(), 200);

        // Assert
        assertNull(nullResult);
        assertTrue(emptyResult.isEmpty());
    }

    // ==================== getTrendingProjects ====================

    @Test
    @DisplayName("获取热门项目 - 正常流程")
    void getTrendingProjects_Success() {
        // Arrange
        when(projectMapper.selectTrendingProjects(10)).thenReturn(List.of(testProject));
        when(projectTagService.getProjectTagsBatch(anyList())).thenReturn(Map.of());
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(testOwner));

        // Act
        List<Project> result = projectService.getTrendingProjects(10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(projectMapper).selectTrendingProjects(10);
    }

    @Test
    @DisplayName("获取热门项目 - limit为null默认10")
    void getTrendingProjects_NullLimit() {
        // Arrange
        when(projectMapper.selectTrendingProjects(10)).thenReturn(List.of());

        // Act
        List<Project> result = projectService.getTrendingProjects(null);

        // Assert
        assertNotNull(result);
        verify(projectMapper).selectTrendingProjects(10);
    }

    @Test
    @DisplayName("获取热门项目 - limit超过50截断")
    void getTrendingProjects_LimitExceedsMax() {
        // Arrange
        when(projectMapper.selectTrendingProjects(50)).thenReturn(List.of());

        // Act
        List<Project> result = projectService.getTrendingProjects(100);

        // Assert
        assertNotNull(result);
        verify(projectMapper).selectTrendingProjects(50);
    }

    // ==================== incrementViewCount ====================

    @Test
    @DisplayName("增加浏览量 - 正常流程")
    void incrementViewCount_Success() {
        // Arrange
        when(projectMapper.incrementViewCount(1)).thenReturn(1);

        // Act - 不应抛出异常
        projectService.incrementViewCount(1);

        // Assert
        verify(projectMapper).incrementViewCount(1);
    }

    @Test
    @DisplayName("增加浏览量 - 异常时不影响调用方")
    void incrementViewCount_Exception() {
        // Arrange
        doThrow(new RuntimeException("数据库错误")).when(projectMapper).incrementViewCount(1);

        // Act - 不应抛出异常
        projectService.incrementViewCount(1);

        // Assert
        verify(projectMapper).incrementViewCount(1);
    }

    // ==================== incrementDownloadCount ====================

    @Test
    @DisplayName("增加下载量 - 正常流程")
    void incrementDownloadCount_Success() {
        // Arrange
        when(projectMapper.incrementDownloadCount(1)).thenReturn(1);

        // Act
        projectService.incrementDownloadCount(1);

        // Assert
        verify(projectMapper).incrementDownloadCount(1);
    }

    // ==================== isProjectOwner ====================

    @Test
    @DisplayName("检查项目所有者 - 是所有者")
    void isProjectOwner_True() {
        // Arrange
        when(projectMapper.selectById(1)).thenReturn(testProject);

        // Act
        boolean result = projectService.isProjectOwner(1, 100);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("检查项目所有者 - 不是所有者")
    void isProjectOwner_False() {
        // Arrange
        when(projectMapper.selectById(1)).thenReturn(testProject);

        // Act
        boolean result = projectService.isProjectOwner(1, 999);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("检查项目所有者 - 项目不存在")
    void isProjectOwner_NotFound() {
        // Arrange
        when(projectMapper.selectById(99)).thenReturn(null);

        // Act
        boolean result = projectService.isProjectOwner(99, 100);

        // Assert
        assertFalse(result);
    }

    // ==================== countByUserId ====================

    @Test
    @DisplayName("统计用户项目数 - 正常流程")
    void countByUserId_Success() {
        // Arrange
        when(projectMapper.countByUserId(100)).thenReturn(5);

        // Act
        long result = projectService.countByUserId(100);

        // Assert
        assertEquals(5L, result);
    }

    // ==================== countPublicProjects ====================

    @Test
    @DisplayName("统计公开项目数 - 正常流程")
    void countPublicProjects_Success() {
        // Arrange
        when(projectMapper.countPublicProjects()).thenReturn(20);

        // Act
        long result = projectService.countPublicProjects();

        // Assert
        assertEquals(20L, result);
    }
}
