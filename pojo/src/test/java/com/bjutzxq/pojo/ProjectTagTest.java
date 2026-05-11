package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectTag 实体类测试
 */
class ProjectTagTest {

    @Test
    void testConstructorAndGetters() {
        ProjectTag projectTag = new ProjectTag();
        assertNull(projectTag.getProjectId());
        assertNull(projectTag.getTagId());
        assertNull(projectTag.getCreatedAt());
    }

    @Test
    void testSetters() {
        ProjectTag projectTag = new ProjectTag();
        LocalDateTime now = LocalDateTime.now();
        
        projectTag.setProjectId(100);
        projectTag.setTagId(200);
        projectTag.setCreatedAt(now);

        assertEquals(100, projectTag.getProjectId());
        assertEquals(200, projectTag.getTagId());
        assertEquals(now, projectTag.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        
        ProjectTag pt1 = new ProjectTag();
        pt1.setProjectId(100);
        pt1.setTagId(200);
        pt1.setCreatedAt(now);

        ProjectTag pt2 = new ProjectTag();
        pt2.setProjectId(100);
        pt2.setTagId(200);
        pt2.setCreatedAt(now);

        assertEquals(pt1, pt2);
        assertEquals(pt1.hashCode(), pt2.hashCode());
    }

    @Test
    void testToString() {
        ProjectTag projectTag = new ProjectTag();
        projectTag.setProjectId(100);
        projectTag.setTagId(200);

        String toString = projectTag.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ProjectTag"));
        assertTrue(toString.contains("projectId=100"));
        assertTrue(toString.contains("tagId=200"));
    }

    @Test
    void testCompositeKey() {
        ProjectTag pt1 = new ProjectTag();
        pt1.setProjectId(1);
        pt1.setTagId(2);

        ProjectTag pt2 = new ProjectTag();
        pt2.setProjectId(1);
        pt2.setTagId(3);

        assertNotEquals(pt1, pt2);
    }
}
