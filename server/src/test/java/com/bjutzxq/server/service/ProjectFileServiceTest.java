package com.bjutzxq.server.service;

import com.bjutzxq.pojo.entity.ProjectFile;
import com.bjutzxq.server.mapper.ProjectFileMapper;
import com.bjutzxq.server.util.OssUtil;
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
@DisplayName("项目文件服务测试")
class ProjectFileServiceTest {

    @Mock
    private ProjectFileMapper projectFileMapper;

    @Mock
    private OssUtil ossUtil;

    @InjectMocks
    private ProjectFileService projectFileService;

    // ==================== getAllFiles ====================

    @Test
    @DisplayName("获取项目所有文件成功 - 正常流程")
    void getAllFiles_Success() {
        // Arrange
        ProjectFile pf = new ProjectFile();
        pf.setId(1);
        pf.setFileName("test.pdf");
        when(projectFileMapper.selectByProjectId(10)).thenReturn(List.of(pf));

        // Act
        List<ProjectFile> result = projectFileService.getAllFiles(10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.pdf", result.get(0).getFileName());
        verify(projectFileMapper).selectByProjectId(10);
    }

    @Test
    @DisplayName("获取项目所有文件 - 空结果")
    void getAllFiles_Empty() {
        // Arrange
        when(projectFileMapper.selectByProjectId(10)).thenReturn(List.of());

        // Act
        List<ProjectFile> result = projectFileService.getAllFiles(10);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== deleteAllProjectFiles ====================

    @Test
    @DisplayName("删除项目所有文件 - 无文件直接返回")
    void deleteAllProjectFiles_NoFiles() {
        // Arrange
        when(projectFileMapper.selectByProjectId(10)).thenReturn(List.of());

        // Act
        int result = projectFileService.deleteAllProjectFiles(10);

        // Assert
        assertEquals(0, result);
        verify(ossUtil, never()).delete(anyString());
    }

    @Test
    @DisplayName("删除项目所有文件成功 - 有文件有OSS记录")
    void deleteAllProjectFiles_WithFiles() {
        // Arrange
        ProjectFile file1 = new ProjectFile();
        file1.setId(1);
        file1.setFileName("test.pdf");
        file1.setStorageUrl("https://oss.example.com/files/test.pdf");
        file1.setIsDir(0);

        ProjectFile dir = new ProjectFile();
        dir.setId(2);
        dir.setFileName("subdir");
        dir.setIsDir(1); // directory - should be skipped for OSS deletion

        when(projectFileMapper.selectByProjectId(10)).thenReturn(List.of(file1, dir));
        doNothing().when(ossUtil).delete(anyString());
        when(projectFileMapper.deleteByProjectId(10)).thenReturn(2);

        // Act
        int result = projectFileService.deleteAllProjectFiles(10);

        // Assert
        assertEquals(2, result);
        // Only the file (not directory) should have its OSS URL deleted
        verify(ossUtil, times(1)).delete("https://oss.example.com/files/test.pdf");
        verify(projectFileMapper).deleteByProjectId(10);
    }

    @Test
    @DisplayName("删除项目所有文件 - null项目文件列表返回0")
    void deleteAllProjectFiles_NullList() {
        // Arrange
        when(projectFileMapper.selectByProjectId(10)).thenReturn(null);

        // Act
        int result = projectFileService.deleteAllProjectFiles(10);

        // Assert
        assertEquals(0, result);
    }
}
