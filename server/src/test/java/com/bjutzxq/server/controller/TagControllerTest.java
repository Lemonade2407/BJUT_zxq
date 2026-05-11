package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.Tag;
import com.bjutzxq.server.service.TagService;
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
@DisplayName("标签控制器测试")
class TagControllerTest {

    @Mock
    private TagService tagService;

    @InjectMocks
    private TagController tagController;

    private Tag testTag;

    @BeforeEach
    void setUp() {
        testTag = new Tag();
        testTag.setId(1);
        testTag.setName("Java");
        testTag.setCategory("技术栈");
    }

    @Test
    @DisplayName("创建标签成功 - 正常流程")
    void createTag_Success() {
        // Arrange
        when(tagService.createTag(any(Tag.class))).thenReturn(testTag);

        // Act
        Result<Tag> result = tagController.createTag("Java", "技术栈");

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(tagService).createTag(any(Tag.class));
    }

    @Test
    @DisplayName("更新标签成功 - 正常流程")
    void updateTag_Success() {
        // Arrange
        when(tagService.updateTag(any(Tag.class))).thenReturn(testTag);

        // Act
        Result<Tag> result = tagController.updateTag(1, "Java 17", "技术栈");

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("删除标签成功 - 正常流程")
    void deleteTag_Success() {
        // Arrange
        when(tagService.deleteTag(1)).thenReturn(true);

        // Act
        Result<Boolean> result = tagController.deleteTag(1);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData());
    }

    @Test
    @DisplayName("查询标签成功 - 正常流程")
    void getTagById_Success() {
        // Arrange
        when(tagService.getTagById(1)).thenReturn(testTag);

        // Act
        Result<Tag> result = tagController.getTagById(1);

        // Assert
        assertNotNull(result);
        assertEquals("Java", result.getData().getName());
    }

    @Test
    @DisplayName("查询标签失败 - 标签不存在")
    void getTagById_NotFound() {
        // Arrange
        when(tagService.getTagById(99)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tagController.getTagById(99));
        assertTrue(exception.getMessage().contains("标签不存在"));
    }

    @Test
    @DisplayName("获取所有标签成功 - 正常流程")
    void getAllTags_Success() {
        // Arrange
        when(tagService.getAllTags(anyInt(), anyInt())).thenReturn(List.of(testTag));
        when(tagService.countAllTags()).thenReturn(1L);

        // Act
        var result = tagController.getAllTags(1, 10);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("按分组查询标签成功 - 正常流程")
    void getTagsByCategory_Success() {
        // Arrange
        when(tagService.getTagsByCategory("技术栈")).thenReturn(List.of(testTag));

        // Act
        Result<List<Tag>> result = tagController.getTagsByCategory("技术栈");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("搜索标签成功 - 正常流程")
    void searchTagsByName_Success() {
        // Arrange
        when(tagService.searchTags("Java")).thenReturn(List.of(testTag));

        // Act
        Result<List<Tag>> result = tagController.searchTagsByName("Java");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("获取热门标签成功 - 正常流程")
    void getHotTags_Success() {
        // Arrange
        when(tagService.getHotTags(10)).thenReturn(List.of(testTag));

        // Act
        Result<List<Tag>> result = tagController.getHotTags(10);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }
}
