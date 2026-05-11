package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Star 实体类测试
 */
class StarTest {

    @Test
    void testConstructorAndGetters() {
        Star star = new Star();
        assertNull(star.getId());
        assertNull(star.getUserId());
        assertNull(star.getProjectId());
        assertNull(star.getCreatedAt());
    }

    @Test
    void testSetters() {
        Star star = new Star();
        LocalDateTime now = LocalDateTime.now();
        
        star.setId(1);
        star.setUserId(100);
        star.setProjectId(200);
        star.setCreatedAt(now);

        assertEquals(1, star.getId());
        assertEquals(100, star.getUserId());
        assertEquals(200, star.getProjectId());
        assertEquals(now, star.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        
        Star star1 = new Star();
        star1.setId(1);
        star1.setUserId(100);
        star1.setProjectId(200);
        star1.setCreatedAt(now);

        Star star2 = new Star();
        star2.setId(1);
        star2.setUserId(100);
        star2.setProjectId(200);
        star2.setCreatedAt(now);

        assertEquals(star1, star2);
        assertEquals(star1.hashCode(), star2.hashCode());
    }

    @Test
    void testToString() {
        Star star = new Star();
        star.setId(1);
        star.setUserId(100);
        star.setProjectId(200);

        String toString = star.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Star"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("userId=100"));
    }

    @Test
    void testDifferentUsers() {
        Star star1 = new Star();
        star1.setUserId(100);
        star1.setProjectId(200);

        Star star2 = new Star();
        star2.setUserId(101);
        star2.setProjectId(200);

        assertNotEquals(star1, star2);
    }

    @Test
    void testDifferentProjects() {
        Star star1 = new Star();
        star1.setUserId(100);
        star1.setProjectId(200);

        Star star2 = new Star();
        star2.setUserId(100);
        star2.setProjectId(201);

        assertNotEquals(star1, star2);
    }
}
