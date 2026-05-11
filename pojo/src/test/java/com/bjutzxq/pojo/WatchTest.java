package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Watch 实体类测试
 */
class WatchTest {

    @Test
    void testConstructorAndGetters() {
        Watch watch = new Watch();
        assertNull(watch.getId());
        assertNull(watch.getUserId());
        assertNull(watch.getProjectId());
        assertNull(watch.getNotificationType());
        assertNull(watch.getCreatedAt());
    }

    @Test
    void testSetters() {
        Watch watch = new Watch();
        LocalDateTime now = LocalDateTime.now();
        
        watch.setId(1);
        watch.setUserId(100);
        watch.setProjectId(200);
        watch.setNotificationType(1);
        watch.setCreatedAt(now);

        assertEquals(1, watch.getId());
        assertEquals(100, watch.getUserId());
        assertEquals(200, watch.getProjectId());
        assertEquals(1, watch.getNotificationType());
        assertEquals(now, watch.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        
        Watch watch1 = new Watch();
        watch1.setId(1);
        watch1.setUserId(100);
        watch1.setProjectId(200);
        watch1.setCreatedAt(now);

        Watch watch2 = new Watch();
        watch2.setId(1);
        watch2.setUserId(100);
        watch2.setProjectId(200);
        watch2.setCreatedAt(now);

        assertEquals(watch1, watch2);
        assertEquals(watch1.hashCode(), watch2.hashCode());
    }

    @Test
    void testToString() {
        Watch watch = new Watch();
        watch.setId(1);
        watch.setUserId(100);
        watch.setProjectId(200);

        String toString = watch.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Watch"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("userId=100"));
    }

    @Test
    void testNotificationTypes() {
        Watch allNotifications = new Watch();
        allNotifications.setNotificationType(1);
        assertEquals(1, allNotifications.getNotificationType());

        Watch importantOnly = new Watch();
        importantOnly.setNotificationType(2);
        assertEquals(2, importantOnly.getNotificationType());
    }

    @Test
    void testDifferentUsers() {
        Watch watch1 = new Watch();
        watch1.setUserId(100);
        watch1.setProjectId(200);

        Watch watch2 = new Watch();
        watch2.setUserId(101);
        watch2.setProjectId(200);

        assertNotEquals(watch1, watch2);
    }

    @Test
    void testDifferentProjects() {
        Watch watch1 = new Watch();
        watch1.setUserId(100);
        watch1.setProjectId(200);

        Watch watch2 = new Watch();
        watch2.setUserId(100);
        watch2.setProjectId(201);

        assertNotEquals(watch1, watch2);
    }

    @Test
    void testUserWatchingMultipleProjects() {
        Watch watch1 = new Watch();
        watch1.setUserId(100);
        watch1.setProjectId(200);

        Watch watch2 = new Watch();
        watch2.setUserId(100);
        watch2.setProjectId(201);

        assertEquals(watch1.getUserId(), watch2.getUserId());
        assertNotEquals(watch1.getProjectId(), watch2.getProjectId());
    }
}
