package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tag 实体类测试
 */
class TagTest {

    @Test
    void testConstructorAndGetters() {
        Tag tag = new Tag();
        assertNull(tag.getId());
        assertNull(tag.getName());
        assertNull(tag.getUsageCount());
    }

    @Test
    void testSetters() {
        Tag tag = new Tag();
        
        tag.setId(1);
        tag.setName("Java");
        tag.setUsageCount(100);

        assertEquals(1, tag.getId());
        assertEquals("Java", tag.getName());
        assertEquals(100, tag.getUsageCount());
    }

    @Test
    void testEqualsAndHashCode() {
        Tag tag1 = new Tag();
        tag1.setId(1);
        tag1.setName("Java");
        tag1.setUsageCount(100);

        Tag tag2 = new Tag();
        tag2.setId(1);
        tag2.setName("Java");
        tag2.setUsageCount(100);

        assertEquals(tag1, tag2);
        assertEquals(tag1.hashCode(), tag2.hashCode());
    }

    @Test
    void testToString() {
        Tag tag = new Tag();
        tag.setId(1);
        tag.setName("Spring");
        tag.setUsageCount(50);

        String toString = tag.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Tag"));
        assertTrue(toString.contains("name=Spring"));
    }

    @Test
    void testDifferentTags() {
        Tag tag1 = new Tag();
        tag1.setName("Java");

        Tag tag2 = new Tag();
        tag2.setName("Python");

        assertNotEquals(tag1, tag2);
    }

    @Test
    void testZeroUsageCount() {
        Tag tag = new Tag();
        tag.setUsageCount(0);
        assertEquals(0, tag.getUsageCount());
    }

    @Test
    void testHighUsageCount() {
        Tag tag = new Tag();
        tag.setUsageCount(9999);
        assertEquals(9999, tag.getUsageCount());
    }

    @Test
    void testVariousTagNames() {
        Tag javaTag = new Tag();
        javaTag.setName("Java");
        assertEquals("Java", javaTag.getName());

        Tag springTag = new Tag();
        springTag.setName("Spring Boot");
        assertEquals("Spring Boot", springTag.getName());

        Tag vueTag = new Tag();
        vueTag.setName("Vue.js");
        assertEquals("Vue.js", vueTag.getName());
    }
}
