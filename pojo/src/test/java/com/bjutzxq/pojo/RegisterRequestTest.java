package com.bjutzxq.pojo;
import com.bjutzxq.pojo.dto.RegisterDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RegisterDTO 实体类测试
 */
class RegisterDTOTest {

    @Test
    void testConstructorAndGetters() {
        RegisterDTO request = new RegisterDTO();
        assertNull(request.getUsername());
        assertNull(request.getPassword());
        assertNull(request.getConfirmPassword());
        assertNull(request.getEmployeeId());
        assertNull(request.getEmail());
        assertNull(request.getPhone());
        assertNull(request.getSex());
        assertNull(request.getBio());
        assertNull(request.getCaptchaSessionId());
        assertNull(request.getCaptchaCode());
    }

    @Test
    void testSetters() {
        RegisterDTO request = new RegisterDTO();
        
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setEmployeeId("2021001");
        request.setEmail("test@example.com");
        request.setPhone("13800138000");
        request.setSex("男");
        request.setBio("这是我的简介");
        request.setCaptchaSessionId("session-123");
        request.setCaptchaCode("ABCD");

        assertEquals("testuser", request.getUsername());
        assertEquals("password123", request.getPassword());
        assertEquals("password123", request.getConfirmPassword());
        assertEquals("2021001", request.getEmployeeId());
        assertEquals("test@example.com", request.getEmail());
        assertEquals("13800138000", request.getPhone());
        assertEquals("男", request.getSex());
        assertEquals("这是我的简介", request.getBio());
        assertEquals("session-123", request.getCaptchaSessionId());
        assertEquals("ABCD", request.getCaptchaCode());
    }

    @Test
    void testEqualsAndHashCode() {
        RegisterDTO request1 = new RegisterDTO();
        request1.setUsername("testuser");
        request1.setEmail("test@example.com");
        request1.setEmployeeId("2021001");

        RegisterDTO request2 = new RegisterDTO();
        request2.setUsername("testuser");
        request2.setEmail("test@example.com");
        request2.setEmployeeId("2021001");

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testToString() {
        RegisterDTO request = new RegisterDTO();
        request.setUsername("testuser");
        request.setEmail("test@example.com");

        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("RegisterDTO"));
        assertTrue(toString.contains("username=testuser"));
    }

    @Test
    void testOptionalFields() {
        RegisterDTO request = new RegisterDTO();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setEmployeeId("2021001");
        request.setEmail("test@example.com");
        // 可选字段为 null
        assertNull(request.getPhone());
        assertNull(request.getSex());
        assertNull(request.getBio());
    }

    @Test
    void testPasswordMismatch() {
        RegisterDTO request = new RegisterDTO();
        request.setPassword("password123");
        request.setConfirmPassword("password456");

        assertNotEquals(request.getPassword(), request.getConfirmPassword());
    }

    @Test
    void testGenderValues() {
        RegisterDTO male = new RegisterDTO();
        male.setSex("男");
        assertEquals("男", male.getSex());

        RegisterDTO female = new RegisterDTO();
        female.setSex("女");
        assertEquals("女", female.getSex());
    }

    @Test
    void testEmptyBio() {
        RegisterDTO request = new RegisterDTO();
        request.setBio("");
        assertEquals("", request.getBio());
    }
}
