package com.bjutzxq.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Role 枚举测试
 */
class RoleTest {

    @Test
    void testRoleValues() {
        // 验证所有角色都存在
        Role[] roles = Role.values();
        assertEquals(3, roles.length);
    }

    @Test
    void testUserRole() {
        assertEquals(1, Role.USER.getCode());
        assertEquals("普通用户", Role.USER.getDescription());
    }

    @Test
    void testTeacherRole() {
        assertEquals(2, Role.TEACHER.getCode());
        assertEquals("教师", Role.TEACHER.getDescription());
    }

    @Test
    void testAdminRole() {
        assertEquals(3, Role.ADMIN.getCode());
        assertEquals("管理员", Role.ADMIN.getDescription());
    }

    @Test
    void testValueOfWithValidCode() {
        assertEquals(Role.USER, Role.valueOf(1));
        assertEquals(Role.TEACHER, Role.valueOf(2));
        assertEquals(Role.ADMIN, Role.valueOf(3));
    }

    @Test
    void testValueOfWithInvalidCode() {
        // 测试无效代码抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            Role.valueOf(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Role.valueOf(4);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Role.valueOf(-1);
        });
    }

    @Test
    void testValueOfWithErrorMessage() {
        try {
            Role.valueOf(999);
            fail("应该抛出 IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("999"));
            assertTrue(e.getMessage().contains("未知的角色代码"));
        }
    }

    @Test
    void testEnumName() {
        assertEquals("USER", Role.USER.name());
        assertEquals("TEACHER", Role.TEACHER.name());
        assertEquals("ADMIN", Role.ADMIN.name());
    }

    @Test
    void testEnumOrdinal() {
        assertEquals(0, Role.USER.ordinal());
        assertEquals(1, Role.TEACHER.ordinal());
        assertEquals(2, Role.ADMIN.ordinal());
    }
}
