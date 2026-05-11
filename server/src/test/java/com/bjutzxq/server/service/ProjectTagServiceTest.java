package com.bjutzxq.server.service;

import com.bjutzxq.pojo.entity.ProjectTag;
import com.bjutzxq.pojo.entity.Tag;
import com.bjutzxq.server.mapper.ProjectTagMapper;
import com.bjutzxq.server.mapper.TagMapper;
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
@DisplayName("项目标签关联服务测试")
class ProjectTagServiceTest {

    @Mock
    private ProjectTagMapper projectTagMapper;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private ProjectTagService projectTagService;

    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setUp() {
        tag1 = new Tag();
        tag1.setId(1);
        tag1.setName("Java");
        tag1.setUsageCount(5);

        tag2 = new Tag();
        tag2.setId(2);
        tag2.setName("Spring Boot");
        tag2.setUsageCount(3);
    }

    // ==================== setProjectTags ====================

    @Test
    @DisplayName("设置项目标签成功 - 正常流程")
    void setProjectTags_Success() {
        // Arrange
        List<Integer> tagIds = List.of(1, 2);
        when(tagMapper.selectByIds(tagIds)).thenReturn(List.of(tag1, tag2));
        when(projectTagMapper.batchInsert(1, tagIds)).thenReturn(2);
        when(tagMapper.incrementUsageCount(anyInt())).thenReturn(1);

        // Act
        projectTagService.setProjectTags(1, tagIds);

        // Assert
        verify(projectTagMapper).deleteByProjectId(1);
        verify(tagMapper).selectByIds(tagIds);
        verify(projectTagMapper).batchInsert(1, tagIds);
        verify(tagMapper, times(2)).incrementUsageCount(anyInt());
    }

    @Test
    @DisplayName("设置项目标签 - 空标签列表仅删除旧标签")
    void setProjectTags_EmptyList() {
        // Act
        projectTagService.setProjectTags(1, List.of());

        // Assert
        verify(projectTagMapper).deleteByProjectId(1);
        verify(projectTagMapper, never()).batchInsert(anyInt(), anyList());
        verify(tagMapper, never()).incrementUsageCount(anyInt());
    }

    @Test
    @DisplayName("设置项目标签 - null标签列表仅删除旧标签")
    void setProjectTags_NullList() {
        // Act
        projectTagService.setProjectTags(1, null);

        // Assert
        verify(projectTagMapper).deleteByProjectId(1);
        verify(projectTagMapper, never()).batchInsert(anyInt(), anyList());
    }

    @Test
    @DisplayName("设置项目标签失败 - 项目ID为空")
    void setProjectTags_NullProjectId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> projectTagService.setProjectTags(null, List.of(1)));
    }

    @Test
    @DisplayName("设置项目标签失败 - 标签不存在")
    void setProjectTags_TagNotExist() {
        // Arrange
        List<Integer> tagIds = List.of(1, 2, 99);
        when(tagMapper.selectByIds(tagIds)).thenReturn(List.of(tag1, tag2));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectTagService.setProjectTags(1, tagIds));
        assertTrue(exception.getMessage().contains("标签不存在"));
        assertTrue(exception.getMessage().contains("99"));
    }

    // ==================== getProjectTags ====================

    @Test
    @DisplayName("获取项目标签成功 - 正常流程")
    void getProjectTags_Success() {
        // Arrange
        when(projectTagMapper.selectTagIdsByProjectId(1)).thenReturn(List.of(1, 2));
        when(tagMapper.selectByIds(List.of(1, 2))).thenReturn(List.of(tag1, tag2));

        // Act
        List<Tag> result = projectTagService.getProjectTags(1);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(projectTagMapper).selectTagIdsByProjectId(1);
        verify(tagMapper).selectByIds(List.of(1, 2));
    }

    @Test
    @DisplayName("获取项目标签 - 项目无标签返回空列表")
    void getProjectTags_Empty() {
        // Arrange
        when(projectTagMapper.selectTagIdsByProjectId(1)).thenReturn(List.of());

        // Act
        List<Tag> result = projectTagService.getProjectTags(1);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取项目标签 - tagIds为null返回空列表")
    void getProjectTags_NullTagIds() {
        // Arrange
        when(projectTagMapper.selectTagIdsByProjectId(1)).thenReturn(null);

        // Act
        List<Tag> result = projectTagService.getProjectTags(1);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取项目标签失败 - 项目ID为空")
    void getProjectTags_NullProjectId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> projectTagService.getProjectTags(null));
    }

    // ==================== getProjectTagsBatch ====================

    @Test
    @DisplayName("批量获取项目标签成功 - 正常流程")
    void getProjectTagsBatch_Success() {
        // Arrange
        List<Integer> projectIds = List.of(1, 2);
        ProjectTag pt1 = new ProjectTag();
        pt1.setProjectId(1);
        pt1.setTagId(1);
        ProjectTag pt2 = new ProjectTag();
        pt2.setProjectId(1);
        pt2.setTagId(2);
        ProjectTag pt3 = new ProjectTag();
        pt3.setProjectId(2);
        pt3.setTagId(1);
        when(projectTagMapper.selectByProjectIds(projectIds))
                .thenReturn(List.of(pt1, pt2, pt3));
        when(tagMapper.selectByIds(List.of(1, 2))).thenReturn(List.of(tag1, tag2));

        // Act
        Map<Integer, List<Tag>> result = projectTagService.getProjectTagsBatch(projectIds);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.get(1).size());
        assertEquals(1, result.get(2).size());
    }

    @Test
    @DisplayName("批量获取项目标签 - 无映射返回空标签")
    void getProjectTagsBatch_NoMappings() {
        // Arrange
        List<Integer> projectIds = List.of(1, 2);
        when(projectTagMapper.selectByProjectIds(projectIds)).thenReturn(List.of());

        // Act
        Map<Integer, List<Tag>> result = projectTagService.getProjectTagsBatch(projectIds);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(1).isEmpty());
        assertTrue(result.get(2).isEmpty());
    }

    @Test
    @DisplayName("批量获取项目标签 - null或空projectIds返回空map")
    void getProjectTagsBatch_NullOrEmpty() {
        // Act
        Map<Integer, List<Tag>> nullResult = projectTagService.getProjectTagsBatch(null);
        Map<Integer, List<Tag>> emptyResult = projectTagService.getProjectTagsBatch(List.of());

        // Assert
        assertTrue(nullResult.isEmpty());
        assertTrue(emptyResult.isEmpty());
    }

    // ==================== removeProjectTags ====================

    @Test
    @DisplayName("移除项目标签成功 - 正常流程")
    void removeProjectTags_Success() {
        // Arrange
        when(projectTagMapper.selectTagIdsByProjectId(1)).thenReturn(List.of(1, 2));
        when(projectTagMapper.deleteByProjectId(1)).thenReturn(2);
        when(tagMapper.decrementUsageCount(anyInt())).thenReturn(1);

        // Act
        projectTagService.removeProjectTags(1);

        // Assert
        verify(projectTagMapper).selectTagIdsByProjectId(1);
        verify(projectTagMapper).deleteByProjectId(1);
        verify(tagMapper, times(2)).decrementUsageCount(anyInt());
    }

    @Test
    @DisplayName("移除项目标签 - 无标签关联")
    void removeProjectTags_NoTags() {
        // Arrange
        when(projectTagMapper.selectTagIdsByProjectId(1)).thenReturn(List.of());
        when(projectTagMapper.deleteByProjectId(1)).thenReturn(0);

        // Act
        projectTagService.removeProjectTags(1);

        // Assert
        verify(projectTagMapper).deleteByProjectId(1);
        verify(tagMapper, never()).decrementUsageCount(anyInt());
    }

    @Test
    @DisplayName("移除项目标签失败 - 项目ID为空")
    void removeProjectTags_NullProjectId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> projectTagService.removeProjectTags(null));
    }
}
