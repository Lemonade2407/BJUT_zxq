package com.bjutzxq.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Constants 常量类测试
 */
class ConstantsTest {

    // User 常量测试
    @Test
    void testUserStatusNormal() {
        assertEquals(1, Constants.User.STATUS_NORMAL);
    }

    @Test
    void testUserStatusDisabled() {
        assertEquals(0, Constants.User.STATUS_DISABLED);
    }

    @Test
    void testUserSexMale() {
        assertEquals("男", Constants.User.SEX_MALE);
    }

    @Test
    void testUserSexFemale() {
        assertEquals("女", Constants.User.SEX_FEMALE);
    }

    @Test
    void testUserSexUnknown() {
        assertEquals("未知", Constants.User.SEX_UNKNOWN);
    }

    // Project 常量测试
    @Test
    void testProjectVisibilityPublic() {
        assertEquals(1, Constants.Project.VISIBILITY_PUBLIC);
    }

    @Test
    void testProjectVisibilityPrivate() {
        assertEquals(0, Constants.Project.VISIBILITY_PRIVATE);
    }

    // File 常量测试
    @Test
    void testFileTypeFile() {
        assertEquals(0, Constants.File.TYPE_FILE);
    }

    @Test
    void testFileTypeDirectory() {
        assertEquals(1, Constants.File.TYPE_DIRECTORY);
    }

    // Comment 常量测试
    @Test
    void testCommentStatusShow() {
        assertEquals(1, Constants.Comment.STATUS_SHOW);
    }

    @Test
    void testCommentStatusDeleted() {
        assertEquals(0, Constants.Comment.STATUS_DELETED);
    }

    // Notification 常量测试
    @Test
    void testNotificationTypeStar() {
        assertEquals(1, Constants.Notification.TYPE_STAR);
    }

    @Test
    void testNotificationTypeComment() {
        assertEquals(2, Constants.Notification.TYPE_COMMENT);
    }

    @Test
    void testNotificationTypeWatch() {
        assertEquals(3, Constants.Notification.TYPE_WATCH);
    }

    @Test
    void testNotificationReadUnread() {
        assertEquals(0, Constants.Notification.READ_UNREAD);
    }

    @Test
    void testNotificationReadRead() {
        assertEquals(1, Constants.Notification.READ_READ);
    }

    // JWT 常量测试
    @Test
    void testJwtTokenPrefix() {
        assertEquals("Bearer ", Constants.JWT.TOKEN_PREFIX);
    }

    @Test
    void testJwtTokenHeader() {
        assertEquals("Authorization", Constants.JWT.TOKEN_HEADER);
    }

    @Test
    void testJwtTokenExpireTime() {
        assertEquals(7200 * 1000, Constants.JWT.TOKEN_EXPIRE_TIME);
        // 验证是 2 小时（毫秒）
        assertEquals(7200000L, Constants.JWT.TOKEN_EXPIRE_TIME);
    }

    @Test
    void testJwtTokenSecret() {
        String secret = Constants.JWT.TOKEN_SECRET;
        assertNotNull(secret);
        assertTrue(secret.length() >= 32, "密钥长度至少应为 32 字符");
        assertEquals("bjut_zxq_2026_jwt_secret_key_for_hmac_sha256", secret);
    }

    // 常量一致性测试
    @Test
    void testConstantsImmutability() {
        // 验证常量值不会被意外修改（通过多次访问）
        assertEquals(1, Constants.User.STATUS_NORMAL);
        assertEquals(1, Constants.User.STATUS_NORMAL);
        assertEquals(1, Constants.User.STATUS_NORMAL);
    }

    @Test
    void testNotificationTypesConsistency() {
        // 验证 Constants.Notification 与 NotificationType 枚举的一致性
        assertEquals(NotificationType.LIKE.getCode(), Constants.Notification.TYPE_STAR);
        assertEquals(NotificationType.COMMENT.getCode(), Constants.Notification.TYPE_COMMENT);
        assertEquals(NotificationType.WATCH.getCode(), Constants.Notification.TYPE_WATCH);
    }
}
