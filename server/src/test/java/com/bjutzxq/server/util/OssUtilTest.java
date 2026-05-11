package com.bjutzxq.server.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OSS工具测试")
class OssUtilTest {

    @Mock
    private OSS ossClient;

    @InjectMocks
    private OssUtil ossUtil;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射设置 @Value 字段
        setField("endpoint", "oss-cn-beijing.aliyuncs.com");
        setField("accessKeyId", "test-key-id");
        setField("accessKeySecret", "test-key-secret");
        setField("bucketName", "test-bucket");
        setField("fileHost", "https://test-bucket.oss-cn-beijing.aliyuncs.com/");
        setField("maxFileSize", 104857600L);
        setField("allowedTypesStr", "jpg,png,gif,pdf,doc,docx,zip,txt,java,py");

        // 设置 OSS 客户端到 OssUtil 实例
        setField("ossClient", ossClient);
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = OssUtil.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(ossUtil, value);
    }

    // ==================== upload ====================

    @Test
    @DisplayName("上传文件成功 - 正常流程（小文件）")
    void upload_SmallFile() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));

        // Act - ossClient.putObject 不mock，Mockito返回null但不影响url构建
        String result = ossUtil.upload(file, "files");

        // Assert - 文件校验通过且返回了正确的URL格式
        assertNotNull(result);
        assertTrue(result.contains("test-bucket"));
    }

    @Test
    @DisplayName("上传文件失败 - 文件类型不支持")
    void upload_InvalidExtension() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.exe");
        when(file.getSize()).thenReturn(1024L);

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> ossUtil.upload(file));
        assertTrue(exception.getMessage().contains("不支持的文件类型"));
    }

    @Test
    @DisplayName("上传文件失败 - 文件超过大小限制")
    void upload_FileTooLarge() throws Exception {
        // Arrange
        setField("maxFileSize", 1024L); // 限制1KB
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("large.pdf");
        when(file.getSize()).thenReturn(2048L); // 2KB > 1KB限制

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> ossUtil.upload(file));
        assertTrue(exception.getMessage().contains("文件大小不能超过"));
    }

    // ==================== delete ====================

    @Test
    @DisplayName("删除文件成功 - 正常流程")
    void delete_Success() {
        // Arrange
        String fileUrl = "https://test-bucket.oss-cn-beijing.aliyuncs.com/files/test-file.pdf";

        // Act & Assert - 不应抛出异常
        assertDoesNotThrow(() -> ossUtil.delete(fileUrl));
    }

    @Test
    @DisplayName("删除文件 - null URL 不执行操作")
    void delete_NullUrl() {
        // Act & Assert - 不应抛出异常
        assertDoesNotThrow(() -> ossUtil.delete(null));
        verify(ossClient, never()).deleteObject(anyString(), anyString());
    }

    // ==================== download ====================

    @Test
    @DisplayName("下载文件成功 - 正常流程")
    void download_Success() throws IOException {
        // Arrange
        String fileUrl = "https://test-bucket.oss-cn-beijing.aliyuncs.com/files/test-file.pdf";
        OSSObject ossObject = new OSSObject();
        ossObject.setObjectContent(new ByteArrayInputStream("file-content".getBytes()));
        when(ossClient.getObject(eq("test-bucket"), eq("files/test-file.pdf"))).thenReturn(ossObject);

        // Act
        byte[] result = ossUtil.download(fileUrl);

        // Assert
        assertNotNull(result);
        assertEquals("file-content", new String(result));
    }

    @Test
    @DisplayName("下载文件失败 - null URL")
    void download_NullUrl() {
        // Act & Assert
        assertThrows(IOException.class, () -> ossUtil.download(null));
    }

    // ==================== batchDelete ====================

    @Test
    @DisplayName("批量删除文件成功 - 正常流程")
    void batchDelete_Success() {
        // Arrange
        String url1 = "https://test-bucket.oss-cn-beijing.aliyuncs.com/files/file1.pdf";
        String url2 = "https://test-bucket.oss-cn-beijing.aliyuncs.com/files/file2.pdf";
        DeleteObjectsResult deleteResult = mock(DeleteObjectsResult.class);
        when(deleteResult.getDeletedObjects()).thenReturn(List.of("files/file1.pdf", "files/file2.pdf"));
        when(ossClient.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteResult);

        // Act
        int result = ossUtil.batchDelete(List.of(url1, url2));

        // Assert
        assertEquals(2, result);
    }
}
