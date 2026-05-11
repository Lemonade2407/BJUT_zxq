package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import com.bjutzxq.common.Role;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * User 实体类测试
 */
class UserTest {

    @Test
    void testConstructorAndGetters() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getEmployeeId());
        assertNull(user.getEmail());
        assertNull(user.getAvatar());
        assertNull(user.getPhone());
        assertNull(user.getSex());
        assertNull(user.getBio());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
        assertNull(user.getStatus());
        assertNull(user.getRole());
    }

    @Test
    void testSetters() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();
        
        user.setId(1);
        user.setUsername("testuser");
        user.setPassword("encrypted_password");
        user.setEmployeeId("2021001");
        user.setEmail("test@example.com");
        user.setAvatar("/avatars/default.png");
        user.setPhone("13800138000");
        user.setSex("男");
        user.setBio("个人简介");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setStatus(1);
        user.setRole(Role.USER);

        assertEquals(1, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("encrypted_password", user.getPassword());
        assertEquals("2021001", user.getEmployeeId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("/avatars/default.png", user.getAvatar());
        assertEquals("13800138000", user.getPhone());
        assertEquals("男", user.getSex());
        assertEquals("个人简介", user.getBio());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
        assertEquals(1, user.getStatus());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        
        User user1 = new User();
        user1.setId(1);
        user1.setUsername("testuser");
        user1.setEmail("test@example.com");
        user1.setCreatedAt(now);

        User user2 = new User();
        user2.setId(1);
        user2.setUsername("testuser");
        user2.setEmail("test@example.com");
        user2.setCreatedAt(now);

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void testToString() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        String toString = user.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("User"));
        assertTrue(toString.contains("username=testuser"));
    }

    @Test
    void testUserRoles() {
        User admin = new User();
        admin.setRole(Role.ADMIN);
        assertEquals(Role.ADMIN, admin.getRole());

        User user = new User();
        user.setRole(Role.USER);
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void testUserStatus() {
        User activeUser = new User();
        activeUser.setStatus(1);
        assertEquals(1, activeUser.getStatus());

        User disabledUser = new User();
        disabledUser.setStatus(0);
        assertEquals(0, disabledUser.getStatus());
    }

    @Test
    void testOptionalFields() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmployeeId("2021001");
        user.setEmail("test@example.com");
        // 可选字段为 null
        assertNull(user.getAvatar());
        assertNull(user.getPhone());
        assertNull(user.getSex());
        assertNull(user.getBio());
    }

    @Test
    void testDefaultAvatar() {
        User user = new User();
        user.setAvatar("/avatars/default.png");
        assertEquals("/avatars/default.png", user.getAvatar());
    }

    @Test
    void testEmptyBio() {
        User user = new User();
        user.setBio("");
        assertEquals("", user.getBio());
    }

    @Test
    void testGenderValues() {
        User male = new User();
        male.setSex("男");
        assertEquals("男", male.getSex());

        User female = new User();
        female.setSex("女");
        assertEquals("女", female.getSex());
    }

    @Test
    void testDifferentUsers() {
        User user1 = new User();
        user1.setId(1);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(2);
        user2.setUsername("user2");

        assertNotEquals(user1, user2);
    }
}
