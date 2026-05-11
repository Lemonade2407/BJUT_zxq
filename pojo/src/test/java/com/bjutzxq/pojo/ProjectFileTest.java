package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectFile 实体类测试
 */
class ProjectFileTest {

    @Test
    void testConstructorAndGetters() {
        ProjectFile file = new ProjectFile();
        assertNull(file.getId());
        assertNull(file.getProjectId());
        assertNull(file.getFileName());
        assertNull(file.getFilePath());
        assertNull(file.getFileSize());
        assertNull(file.getFileType());
        assertNull(file.getStorageUrl());
        assertNull(file.getContent());
        assertNull(file.getIsDir());
        assertNull(file.getParentId());
        assertNull(file.getUploaderId());
        assertNull(file.getCreatedAt());
        assertNull(file.getUpdatedAt());
    }

    @Test
    void testSetters() {
        ProjectFile file = new ProjectFile();
        LocalDateTime now = LocalDateTime.now();
        
        file.setId(1);
        file.setProjectId(100);
        file.setFileName("Test.java");
        file.setFilePath("/src/main/java/Test.java");
        file.setFileSize(1024L);
        file.setFileType("java");
        file.setStorageUrl("/storage/test.java");
        file.setContent("public class Test {}");
        file.setIsDir(0);
        file.setParentId(null);
        file.setUploaderId(200);
        file.setCreatedAt(now);
        file.setUpdatedAt(now);

        assertEquals(1, file.getId());
        assertEquals(100, file.getProjectId());
        assertEquals("Test.java", file.getFileName());
        assertEquals("/src/main/java/Test.java", file.getFilePath());
        assertEquals(Long.valueOf(1024L), file.getFileSize());
        assertEquals("java", file.getFileType());
        assertEquals("/storage/test.java", file.getStorageUrl());
        assertEquals("public class Test {}", file.getContent());
        assertEquals(0, file.getIsDir());
        assertNull(file.getParentId());
        assertEquals(200, file.getUploaderId());
        assertEquals(now, file.getCreatedAt());
        assertEquals(now, file.getUpdatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        
        ProjectFile file1 = new ProjectFile();
        file1.setId(1);
        file1.setFileName("Test.java");
        file1.setProjectId(100);
        file1.setCreatedAt(now);

        ProjectFile file2 = new ProjectFile();
        file2.setId(1);
        file2.setFileName("Test.java");
        file2.setProjectId(100);
        file2.setCreatedAt(now);

        assertEquals(file1, file2);
        assertEquals(file1.hashCode(), file2.hashCode());
    }

    @Test
    void testToString() {
        ProjectFile file = new ProjectFile();
        file.setId(1);
        file.setFileName("Test.java");
        file.setFileSize(2048L);

        String toString = file.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ProjectFile"));
        assertTrue(toString.contains("fileName=Test.java"));
    }

    @Test
    void testDirectoryFile() {
        ProjectFile dir = new ProjectFile();
        dir.setId(1);
        dir.setFileName("src");
        dir.setIsDir(1);
        
        assertEquals(1, dir.getIsDir());
    }

    @Test
    void testRegularFile() {
        ProjectFile file = new ProjectFile();
        file.setId(2);
        file.setFileName("Main.java");
        file.setIsDir(0);
        
        assertEquals(0, file.getIsDir());
    }

    @Test
    void testDifferentFileTypes() {
        ProjectFile javaFile = new ProjectFile();
        javaFile.setFileType("java");
        assertEquals("java", javaFile.getFileType());

        ProjectFile pdfFile = new ProjectFile();
        pdfFile.setFileType("pdf");
        assertEquals("pdf", pdfFile.getFileType());

        ProjectFile zipFile = new ProjectFile();
        zipFile.setFileType("zip");
        assertEquals("zip", zipFile.getFileType());
    }

    @Test
    void testLargeFileSize() {
        ProjectFile file = new ProjectFile();
        file.setFileSize(1073741824L); // 1GB
        assertEquals(Long.valueOf(1073741824L), file.getFileSize());
    }

    @Test
    void testNestedFile() {
        ProjectFile parentDir = new ProjectFile();
        parentDir.setId(1);
        parentDir.setFileName("src");
        parentDir.setIsDir(1);

        ProjectFile childFile = new ProjectFile();
        childFile.setId(2);
        childFile.setFileName("Main.java");
        childFile.setParentId(1);
        childFile.setIsDir(0);

        assertEquals(parentDir.getId(), childFile.getParentId());
    }

    @Test
    void testNullContent() {
        ProjectFile file = new ProjectFile();
        file.setContent(null);
        assertNull(file.getContent());
    }

    @Test
    void testEmptyContent() {
        ProjectFile file = new ProjectFile();
        file.setContent("");
        assertEquals("", file.getContent());
    }
}
