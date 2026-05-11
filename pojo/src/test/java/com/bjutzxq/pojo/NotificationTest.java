package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Notification 实体类测试
 */
class NotificationTest {

    @Test
    void testConstructorAndGetters() {
        Notification notification = new Notification();
        assertNull(notification.getId());
        assertNull(notification.getUserId());
        assertNull(notification.getSenderId());
        assertNull(notification.getProjectId());
        assertNull(notification.getType());
        assertNull(notification.getContent());
        assertNull(notification.getIsRead());
        assertNull(notification.getCreatedAt());
    }

    @Test
    void testSetters() {
        Notification notification = new Notification();
        LocalDateTime now = LocalDateTime.now();
        
        notification.setId(1);
        notification.setUserId(100);
        notification.setSenderId(200);
        notification.setProjectId(300);
        notification.setType(1);
        notification.setContent("用户点赞了你的项目");
        notification.setIsRead(0);
        notification.setCreatedAt(now);

        assertEquals(1, notification.getId());
        assertEquals(100, notification.getUserId());
        assertEquals(200, notification.getSenderId());
        assertEquals(300, notification.getProjectId());
        assertEquals(1, notification.getType());
        assertEquals("用户点赞了你的项目", notification.getContent());
        assertEquals(0, notification.getIsRead());
        assertEquals(now, notification.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        
        Notification notif1 = new Notification();
        notif1.setId(1);
        notif1.setUserId(100);
        notif1.setContent("测试通知");
        notif1.setCreatedAt(now);

        Notification notif2 = new Notification();
        notif2.setId(1);
        notif2.setUserId(100);
        notif2.setContent("测试通知");
        notif2.setCreatedAt(now);

        assertEquals(notif1, notif2);
        assertEquals(notif1.hashCode(), notif2.hashCode());
    }

    @Test
    void testToString() {
        Notification notification = new Notification();
        notification.setId(1);
        notification.setContent("新通知");
        notification.setIsRead(0);

        String toString = notification.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Notification"));
        assertTrue(toString.contains("content=新通知"));
    }

    @Test
    void testNotificationTypes() {
        Notification likeNotif = new Notification();
        likeNotif.setType(1);
        assertEquals(1, likeNotif.getType());

        Notification commentNotif = new Notification();
        commentNotif.setType(2);
        assertEquals(2, commentNotif.getType());

        Notification followNotif = new Notification();
        followNotif.setType(3);
        assertEquals(3, followNotif.getType());
    }

    @Test
    void testReadStatus() {
        Notification unreadNotif = new Notification();
        unreadNotif.setIsRead(0);
        assertEquals(0, unreadNotif.getIsRead());

        Notification readNotif = new Notification();
        readNotif.setIsRead(1);
        assertEquals(1, readNotif.getIsRead());
    }

    @Test
    void testNullProjectId() {
        Notification notification = new Notification();
        notification.setProjectId(null);
        assertNull(notification.getProjectId());
    }
}
