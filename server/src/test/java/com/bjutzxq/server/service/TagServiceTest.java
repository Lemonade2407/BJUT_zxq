package com.bjutzxq.server.service;

import com.bjutzxq.common.BusinessException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("标签服务测试")
class TagServiceTest {

    @Mock
    private TagMapper tagMapper;

    @Mock
    private ProjectTagMapper projectTagMapper;

    @InjectMocks
    private TagService tagService;

    private Tag testTag;

    @BeforeEach
    void setUp() {
        testTag = new Tag();
        testTag.setId(1);
        testTag.setName("Java");
        testTag.setCategory("技术栈");
        testTag.setUsageCount(5);
    }

    // ==================== createTag ====================

    @Test
    @DisplayName("创建标签成功 - 正常流程")
    void createTag_Success() {
        // Arrange
        Tag newTag = new Tag();
        newTag.setName("Python");
        newTag.setCategory("技术栈");
        when(tagMapper.countByNameExcludeId("Python", null)).thenReturn(0);
        when(tagMapper.insert(any(Tag.class))).thenAnswer(invocation -> {
            Tag t = invocation.getArgument(0);
            t.setId(2);
            return 1;
        });

        // Act
        Tag result = tagService.createTag(newTag);

        // Assert
        assertNotNull(result);
        assertEquals("Python", result.getName());
        assertEquals(2, result.getId());
        assertEquals(0, result.getUsageCount());
        verify(tagMapper).countByNameExcludeId("Python", null);
        verify(tagMapper).insert(any(Tag.class));
    }

    @Test
    @DisplayName("创建标签成功 - 未设置使用次数默认为0")
    void createTag_DefaultUsageCount() {
        // Arrange
        Tag newTag = new Tag();
        newTag.setName("Python");
        when(tagMapper.countByNameExcludeId("Python", null)).thenReturn(0);
        when(tagMapper.insert(any(Tag.class))).thenReturn(1);

        // Act
        Tag result = tagService.createTag(newTag);

        // Assert
        assertEquals(0, result.getUsageCount());
    }

    @Test
    @DisplayName("创建标签失败 - 标签名称为空")
    void createTag_EmptyName() {
        // Arrange
        Tag emptyTag = new Tag();
        emptyTag.setName("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> tagService.createTag(emptyTag));
    }

    @Test
    @DisplayName("创建标签失败 - 标签名称为null")
    void createTag_NullName() {
        // Act & Assert
        Tag nullTag = new Tag();
        assertThrows(IllegalArgumentException.class,
                () -> tagService.createTag(nullTag));
    }

    @Test
    @DisplayName("创建标签失败 - 标签名称已存在")
    void createTag_DuplicateName() {
        // Arrange
        Tag newTag = new Tag();
        newTag.setName("Java");
        when(tagMapper.countByNameExcludeId("Java", null)).thenReturn(1);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> tagService.createTag(newTag));
        assertEquals(409, exception.getCode());
        assertEquals("标签名称已存在", exception.getMessage());
        verify(tagMapper, never()).insert(any(Tag.class));
    }

    // ==================== updateTag ====================

    @Test
    @DisplayName("更新标签成功 - 正常流程")
    void updateTag_Success() {
        // Arrange
        Tag update = new Tag();
        update.setId(1);
        update.setName("Java 17");
        when(tagMapper.selectById(1)).thenReturn(testTag);
        when(tagMapper.countByNameExcludeId("Java 17", 1)).thenReturn(0);
        when(tagMapper.update(any(Tag.class))).thenReturn(1);

        // Act
        Tag result = tagService.updateTag(update);

        // Assert
        assertNotNull(result);
        assertEquals("Java 17", result.getName());
        verify(tagMapper).selectById(1);
        verify(tagMapper).countByNameExcludeId("Java 17", 1);
        verify(tagMapper).update(any(Tag.class));
    }

    @Test
    @DisplayName("更新标签 - 名称未改变不检查重名")
    void updateTag_SameName() {
        // Arrange
        Tag update = new Tag();
        update.setId(1);
        update.setName("Java");
        when(tagMapper.selectById(1)).thenReturn(testTag);
        when(tagMapper.update(any(Tag.class))).thenReturn(1);

        // Act
        Tag result = tagService.updateTag(update);

        // Assert
        assertNotNull(result);
        verify(tagMapper, never()).countByNameExcludeId(anyString(), any());
        verify(tagMapper).update(any(Tag.class));
    }

    @Test
    @DisplayName("更新标签失败 - 标签ID为空")
    void updateTag_NullId() {
        // Act & Assert
        Tag update = new Tag();
        assertThrows(IllegalArgumentException.class,
                () -> tagService.updateTag(update));
    }

    @Test
    @DisplayName("更新标签失败 - 标签不存在")
    void updateTag_NotFound() {
        // Arrange
        Tag update = new Tag();
        update.setId(99);
        when(tagMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tagService.updateTag(update));
        assertEquals("标签不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新标签失败 - 新名称已被使用")
    void updateTag_DuplicateName() {
        // Arrange
        Tag update = new Tag();
        update.setId(1);
        update.setName("Python");
        when(tagMapper.selectById(1)).thenReturn(testTag);
        when(tagMapper.countByNameExcludeId("Python", 1)).thenReturn(1);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tagService.updateTag(update));
        assertEquals("标签名称已存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新标签失败 - 更新时名称为空")
    void updateTag_EmptyNameInUpdate() {
        // Arrange
        Tag update = new Tag();
        update.setId(1);
        update.setName("");
        when(tagMapper.selectById(1)).thenReturn(testTag);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> tagService.updateTag(update));
    }

    // ==================== deleteTag ====================

    @Test
    @DisplayName("删除标签成功 - 正常流程")
    void deleteTag_Success() {
        // Arrange
        when(tagMapper.selectById(1)).thenReturn(testTag);
        when(projectTagMapper.selectProjectIdsByTagId(1)).thenReturn(List.of());
        when(tagMapper.deleteById(1)).thenReturn(1);

        // Act
        boolean result = tagService.deleteTag(1);

        // Assert
        assertTrue(result);
        verify(tagMapper).selectById(1);
        verify(projectTagMapper).selectProjectIdsByTagId(1);
        verify(tagMapper).deleteById(1);
    }

    @Test
    @DisplayName("删除标签失败 - 标签不存在")
    void deleteTag_NotFound() {
        // Arrange
        when(tagMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tagService.deleteTag(99));
        assertEquals("标签不存在", exception.getMessage());
        verify(tagMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("删除标签失败 - 标签正在被项目使用")
    void deleteTag_HasProjects() {
        // Arrange
        when(tagMapper.selectById(1)).thenReturn(testTag);
        when(projectTagMapper.selectProjectIdsByTagId(1)).thenReturn(List.of(1, 2, 3));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tagService.deleteTag(1));
        assertTrue(exception.getMessage().contains("无法删除标签"));
        assertTrue(exception.getMessage().contains("3 个项目"));
        verify(tagMapper, never()).deleteById(anyInt());
    }

    // ==================== getTagById ====================

    @Test
    @DisplayName("查询标签成功 - 正常流程")
    void getTagById_Success() {
        // Arrange
        when(tagMapper.selectById(1)).thenReturn(testTag);

        // Act
        Tag result = tagService.getTagById(1);

        // Assert
        assertNotNull(result);
        assertEquals("Java", result.getName());
    }

    @Test
    @DisplayName("查询标签 - 标签不存在返回null")
    void getTagById_NotFound() {
        // Arrange
        when(tagMapper.selectById(99)).thenReturn(null);

        // Act
        Tag result = tagService.getTagById(99);

        // Assert
        assertNull(result);
    }

    // ==================== getAllTags ====================

    @Test
    @DisplayName("分页查询标签成功 - 正常流程")
    void getAllTags_Success() {
        // Arrange
        when(tagMapper.selectAll()).thenReturn(List.of(testTag));

        // Act
        List<Tag> result = tagService.getAllTags(1, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tagMapper).selectAll();
    }

    // ==================== countAllTags ====================

    @Test
    @DisplayName("统计标签总数 - 正常流程")
    void countAllTags_Success() {
        // Arrange
        when(tagMapper.countAll()).thenReturn(10);

        // Act
        long result = tagService.countAllTags();

        // Assert
        assertEquals(10L, result);
        verify(tagMapper).countAll();
    }

    // ==================== getTagsByCategory ====================

    @Test
    @DisplayName("按分组查询标签成功 - 正常流程")
    void getTagsByCategory_Success() {
        // Arrange
        when(tagMapper.selectByCategory("技术栈")).thenReturn(List.of(testTag));

        // Act
        List<Tag> result = tagService.getTagsByCategory("技术栈");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tagMapper).selectByCategory("技术栈");
    }

    @Test
    @DisplayName("按分组查询标签失败 - 分组为空")
    void getTagsByCategory_Empty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> tagService.getTagsByCategory(""));
        assertThrows(IllegalArgumentException.class,
                () -> tagService.getTagsByCategory(null));
    }

    // ==================== searchTags ====================

    @Test
    @DisplayName("搜索标签成功 - 正常流程")
    void searchTags_Success() {
        // Arrange
        when(tagMapper.selectByName("Java")).thenReturn(List.of(testTag));

        // Act
        List<Tag> result = tagService.searchTags("Java");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tagMapper).selectByName("Java");
    }

    @Test
    @DisplayName("搜索标签失败 - 名称为空")
    void searchTags_Empty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> tagService.searchTags(""));
        assertThrows(IllegalArgumentException.class,
                () -> tagService.searchTags(null));
    }

    // ==================== getHotTags ====================

    @Test
    @DisplayName("查询热门标签成功 - 正常流程")
    void getHotTags_Success() {
        // Arrange
        when(tagMapper.selectHotTags(5)).thenReturn(List.of(testTag));

        // Act
        List<Tag> result = tagService.getHotTags(5);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tagMapper).selectHotTags(5);
    }

    @Test
    @DisplayName("查询热门标签 - limit为null使用默认值10")
    void getHotTags_NullLimit() {
        // Arrange
        when(tagMapper.selectHotTags(10)).thenReturn(List.of());

        // Act
        List<Tag> result = tagService.getHotTags(null);

        // Assert
        assertNotNull(result);
        verify(tagMapper).selectHotTags(10);
    }

    @Test
    @DisplayName("查询热门标签 - limit<=0使用默认值10")
    void getHotTags_ZeroLimit() {
        // Arrange
        when(tagMapper.selectHotTags(10)).thenReturn(List.of());

        // Act
        List<Tag> result = tagService.getHotTags(0);

        // Assert
        assertNotNull(result);
        verify(tagMapper).selectHotTags(10);
    }

    @Test
    @DisplayName("查询热门标签 - limit超过50截断")
    void getHotTags_LimitExceedsMax() {
        // Arrange
        when(tagMapper.selectHotTags(50)).thenReturn(List.of());

        // Act
        List<Tag> result = tagService.getHotTags(100);

        // Assert
        assertNotNull(result);
        verify(tagMapper).selectHotTags(50);
    }
}
