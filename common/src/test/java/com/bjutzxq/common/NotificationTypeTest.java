package com.bjutzxq.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationType 枚举测试
 */
class NotificationTypeTest {

    @Test
    void testNotificationTypeValues() {
        // 验证所有通知类型都存在
        NotificationType[] types = NotificationType.values();
        assertEquals(5, types.length);
    }

    @Test
    void testLikeType() {
        assertEquals(1, NotificationType.LIKE.getCode());
        assertEquals("点赞", NotificationType.LIKE.getDescription());
    }

    @Test
    void testCommentType() {
        assertEquals(2, NotificationType.COMMENT.getCode());
        assertEquals("评论", NotificationType.COMMENT.getDescription());
    }

    @Test
    void testWatchType() {
        assertEquals(3, NotificationType.WATCH.getCode());
        assertEquals("关注", NotificationType.WATCH.getDescription());
    }

    @Test
    void testSystemType() {
        assertEquals(4, NotificationType.SYSTEM.getCode());
        assertEquals("系统通知", NotificationType.SYSTEM.getDescription());
    }

    @Test
    void testTeamApplicationType() {
        assertEquals(5, NotificationType.TEAM_APPLICATION.getCode());
        assertEquals("组队申请", NotificationType.TEAM_APPLICATION.getDescription());
    }

    @Test
    void testValueOfWithValidCode() {
        assertEquals(NotificationType.LIKE, NotificationType.valueOf(1));
        assertEquals(NotificationType.COMMENT, NotificationType.valueOf(2));
        assertEquals(NotificationType.WATCH, NotificationType.valueOf(3));
        assertEquals(NotificationType.SYSTEM, NotificationType.valueOf(4));
        assertEquals(NotificationType.TEAM_APPLICATION, NotificationType.valueOf(5));
    }

    @Test
    void testValueOfWithInvalidCode() {
        // 测试无效代码抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            NotificationType.valueOf(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            NotificationType.valueOf(6);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            NotificationType.valueOf(-1);
        });
    }

    @Test
    void testValueOfWithErrorMessage() {
        try {
            NotificationType.valueOf(999);
            fail("应该抛出 IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("999"));
            assertTrue(e.getMessage().contains("未知的通知类型代码"));
        }
    }

    @Test
    void testEnumName() {
        assertEquals("LIKE", NotificationType.LIKE.name());
        assertEquals("COMMENT", NotificationType.COMMENT.name());
        assertEquals("WATCH", NotificationType.WATCH.name());
        assertEquals("SYSTEM", NotificationType.SYSTEM.name());
    }

    @Test
    void testEnumOrdinal() {
        assertEquals(0, NotificationType.LIKE.ordinal());
        assertEquals(1, NotificationType.COMMENT.ordinal());
        assertEquals(2, NotificationType.WATCH.ordinal());
        assertEquals(3, NotificationType.SYSTEM.ordinal());
        assertEquals(4, NotificationType.TEAM_APPLICATION.ordinal());
    }
}
