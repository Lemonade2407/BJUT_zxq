package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Project 实体类测试
 */
class ProjectTest {

    @Test
    void testConstructorAndGetters() {
        Project project = new Project();
        assertNull(project.getId());
        assertNull(project.getName());
        assertNull(project.getDescription());
        assertNull(project.getOwnerId());
        assertNull(project.getVisibility());
        assertNull(project.getStarCount());
        assertNull(project.getWatchCount());
        assertNull(project.getFileCount());
        assertNull(project.getDownloadCount());
        assertNull(project.getViewCount());
        assertNull(project.getCreatedAt());
        assertNull(project.getUpdatedAt());
        assertNull(project.getTags());
        assertNull(project.getIsStarred());
        assertNull(project.getIsWatched());
        assertNull(project.getAuthor());
    }

    @Test
    void testSetters() {
        Project project = new Project();
        LocalDateTime now = LocalDateTime.now();
        
        project.setId(1);
        project.setName("测试项目");
        project.setDescription("这是一个测试项目");
        project.setOwnerId(100);
        project.setVisibility(1);
        project.setStarCount(10);
        project.setWatchCount(5);
        project.setFileCount(3);
        project.setDownloadCount(50);
        project.setViewCount(200);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        project.setIsStarred(true);
        project.setIsWatched(false);
        project.setAuthor("张三");

        assertEquals(1, project.getId());
        assertEquals("测试项目", project.getName());
        assertEquals("这是一个测试项目", project.getDescription());
        assertEquals(100, project.getOwnerId());
        assertEquals(1, project.getVisibility());
        assertEquals(10, project.getStarCount());
        assertEquals(5, project.getWatchCount());
        assertEquals(3, project.getFileCount());
        assertEquals(50, project.getDownloadCount());
        assertEquals(200, project.getViewCount());
        assertEquals(now, project.getCreatedAt());
        assertEquals(now, project.getUpdatedAt());
        assertTrue(project.getIsStarred());
        assertFalse(project.getIsWatched());
        assertEquals("张三", project.getAuthor());
    }

    @Test
    void testTagsList() {
        Project project = new Project();
        List<Tag> tags = new ArrayList<>();
        
        Tag tag1 = new Tag();
        tag1.setId(1);
        tag1.setName("Java");
        tags.add(tag1);
        
        Tag tag2 = new Tag();
        tag2.setId(2);
        tag2.setName("Spring");
        tags.add(tag2);
        
        project.setTags(tags);
        
        assertNotNull(project.getTags());
        assertEquals(2, project.getTags().size());
        assertEquals("Java", project.getTags().get(0).getName());
        assertEquals("Spring", project.getTags().get(1).getName());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        
        Project project1 = new Project();
        project1.setId(1);
        project1.setName("测试项目");
        project1.setOwnerId(100);
        project1.setCreatedAt(now);

        Project project2 = new Project();
        project2.setId(1);
        project2.setName("测试项目");
        project2.setOwnerId(100);
        project2.setCreatedAt(now);

        assertEquals(project1, project2);
        assertEquals(project1.hashCode(), project2.hashCode());
    }

    @Test
    void testToString() {
        Project project = new Project();
        project.setId(1);
        project.setName("测试项目");
        project.setStarCount(10);

        String toString = project.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Project"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("name=测试项目"));
    }

    @Test
    void testVisibilityValues() {
        Project privateProject = new Project();
        privateProject.setVisibility(0);
        assertEquals(0, privateProject.getVisibility());

        Project publicProject = new Project();
        publicProject.setVisibility(1);
        assertEquals(1, publicProject.getVisibility());
    }

    @Test
    void testZeroCounts() {
        Project project = new Project();
        project.setStarCount(0);
        project.setWatchCount(0);
        project.setFileCount(0);
        project.setDownloadCount(0);
        project.setViewCount(0);

        assertEquals(0, project.getStarCount());
        assertEquals(0, project.getWatchCount());
        assertEquals(0, project.getFileCount());
        assertEquals(0, project.getDownloadCount());
        assertEquals(0, project.getViewCount());
    }

    @Test
    void testNullTags() {
        Project project = new Project();
        project.setTags(null);
        assertNull(project.getTags());
    }

    @Test
    void testEmptyTags() {
        Project project = new Project();
        project.setTags(new ArrayList<>());
        assertNotNull(project.getTags());
        assertTrue(project.getTags().isEmpty());
    }
}
