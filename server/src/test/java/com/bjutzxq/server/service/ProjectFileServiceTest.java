package com.bjutzxq.server.service;

import com.bjutzxq.pojo.entity.ProjectFile;
import com.bjutzxq.server.mapper.ProjectFileMapper;
import com.bjutzxq.server.util.OssUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache directoryCache;

    @InjectMocks
    private ProjectFileService projectFileService;

    private static final int FILE = 0; // Constants.File.TYPE_FILE

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache("directoryCache")).thenReturn(directoryCache);
    }

    // ==================== uploadFile ====================

    @Test
    @DisplayName("上传单个文件成功 - 正常流程")
    void uploadFile_Success() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(ossUtil.upload(file)).thenReturn("https://oss.example.com/files/test.pdf");
        when(projectFileMapper.insert(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile pf = invocation.getArgument(0);
            pf.setId(1);
            return 1;
        });

        // Act
        ProjectFile result = projectFileService.uploadFile(10, file, null, 100);

        // Assert
        assertNotNull(result);
        assertEquals("test.pdf", result.getFileName());
        assertEquals("https://oss.example.com/files/test.pdf", result.getStorageUrl());
        assertEquals(10, result.getProjectId());
        verify(ossUtil).upload(file);
        verify(projectFileMapper).insert(any(ProjectFile.class));
    }

    @Test
    @DisplayName("上传文件失败 - 文件为空")
    void uploadFile_EmptyFile() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> projectFileService.uploadFile(10, file, null, 100));
    }

    @Test
    @DisplayName("上传文件失败 - 文件名为空")
    void uploadFile_EmptyFileName() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> projectFileService.uploadFile(10, file, null, 100));
    }

    // ==================== uploadFiles ====================

    @Test
    @DisplayName("批量上传文件成功 - 正常流程")
    void uploadFiles_Success() throws IOException {
        // Arrange
        MultipartFile file1 = mock(MultipartFile.class);
        when(file1.isEmpty()).thenReturn(false);
        when(file1.getOriginalFilename()).thenReturn("doc1.pdf");
        when(file1.getSize()).thenReturn(1024L);

        MultipartFile file2 = mock(MultipartFile.class);
        when(file2.isEmpty()).thenReturn(false);
        when(file2.getOriginalFilename()).thenReturn("doc2.docx");
        when(file2.getSize()).thenReturn(2048L);

        MultipartFile[] files = new MultipartFile[]{file1, file2};

        when(ossUtil.upload(any(MultipartFile.class)))
                .thenReturn("https://oss.example.com/files/doc1.pdf")
                .thenReturn("https://oss.example.com/files/doc2.docx");
        when(projectFileMapper.insert(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile pf = invocation.getArgument(0);
            pf.setId(1);
            return 1;
        });

        // Act
        List<ProjectFile> result = projectFileService.uploadFiles(10, files, null, 100);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(ossUtil, times(2)).upload(any(MultipartFile.class));
        verify(projectFileMapper, times(2)).insert(any(ProjectFile.class));
    }

    @Test
    @DisplayName("批量上传文件 - 含空文件跳过")
    void uploadFiles_SkipsEmpty() throws IOException {
        // Arrange
        MultipartFile validFile = mock(MultipartFile.class);
        when(validFile.isEmpty()).thenReturn(false);
        when(validFile.getOriginalFilename()).thenReturn("valid.pdf");
        when(validFile.getSize()).thenReturn(1024L);

        MultipartFile emptyFile = mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);

        MultipartFile[] files = new MultipartFile[]{validFile, emptyFile};

        when(ossUtil.upload(any(MultipartFile.class)))
                .thenReturn("https://oss.example.com/files/valid.pdf");
        when(projectFileMapper.insert(any(ProjectFile.class))).thenReturn(1);

        // Act
        List<ProjectFile> result = projectFileService.uploadFiles(10, files, null, 100);

        // Assert
        assertEquals(1, result.size());
        verify(ossUtil, times(1)).upload(any(MultipartFile.class));
    }

    @Test
    @DisplayName("批量上传文件 - 含文件夹路径的文件使用带路径上传")
    void uploadFiles_WithFolderPath() throws IOException {
        // Arrange
        MultipartFile folderFile = mock(MultipartFile.class);
        when(folderFile.isEmpty()).thenReturn(false);
        when(folderFile.getOriginalFilename()).thenReturn("subdir/doc.pdf");
        when(folderFile.getSize()).thenReturn(1024L);

        MultipartFile[] files = new MultipartFile[]{folderFile};

        when(ossUtil.upload(any(MultipartFile.class)))
                .thenReturn("https://oss.example.com/files/doc.pdf");
        // For the directory lookup
        when(projectFileMapper.selectByPath(anyInt(), anyString(), anyString())).thenReturn(null);
        when(projectFileMapper.insert(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile pf = invocation.getArgument(0);
            pf.setId(pf.getFileName().equals("subdir") ? 100 : 1);
            return 1;
        });

        // Act
        List<ProjectFile> result = projectFileService.uploadFiles(10, files, null, 100);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        // Called twice: once for directory creation, once for file
        verify(projectFileMapper, atLeast(2)).insert(any(ProjectFile.class));
    }

    @Test
    @DisplayName("批量上传失败 - OSS上传异常抛出包装异常")
    void uploadFiles_OssException() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getSize()).thenReturn(1024L);

        MultipartFile[] files = new MultipartFile[]{file};
        when(ossUtil.upload(any(MultipartFile.class)))
                .thenThrow(new RuntimeException("OSS连接失败"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectFileService.uploadFiles(10, files, null, 100));
        assertTrue(exception.getMessage().contains("批量上传失败"));
    }

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
        file1.setIsDir(FILE);

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
