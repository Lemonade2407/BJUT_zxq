package com.bjutzxq.pojo;

import com.bjutzxq.pojo.dto.ProjectDTO;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectDTO 实体类测试
 */
class ProjectDTOTest {

    @Test
    void testConstructorAndGetters() {
        ProjectDTO request = new ProjectDTO();
        assertNull(request.getName());
        assertNull(request.getDescription());
        assertNull(request.getVisibility());
        assertNull(request.getTagIds());
    }

    @Test
    void testSetters() {
        ProjectDTO request = new ProjectDTO();
        
        request.setName("新项目");
        request.setDescription("项目描述");
        request.setVisibility(1);

        assertEquals("新项目", request.getName());
        assertEquals("项目描述", request.getDescription());
        assertEquals(1, request.getVisibility());
    }

    @Test
    void testTagIdsList() {
        ProjectDTO request = new ProjectDTO();
        List<Integer> tagIds = new ArrayList<>();
        tagIds.add(1);
        tagIds.add(2);
        tagIds.add(3);
        
        request.setTagIds(tagIds);
        
        assertNotNull(request.getTagIds());
        assertEquals(3, request.getTagIds().size());
        assertEquals(Integer.valueOf(1), request.getTagIds().get(0));
        assertEquals(Integer.valueOf(2), request.getTagIds().get(1));
        assertEquals(Integer.valueOf(3), request.getTagIds().get(2));
    }

    @Test
    void testEqualsAndHashCode() {
        ProjectDTO request1 = new ProjectDTO();
        request1.setName("测试项目");
        request1.setDescription("描述");
        request1.setVisibility(1);

        ProjectDTO request2 = new ProjectDTO();
        request2.setName("测试项目");
        request2.setDescription("描述");
        request2.setVisibility(1);

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testToString() {
        ProjectDTO request = new ProjectDTO();
        request.setName("测试项目");
        request.setVisibility(1);

        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ProjectDTO"));
        assertTrue(toString.contains("name=测试项目"));
    }

    @Test
    void testNullTagIds() {
        ProjectDTO request = new ProjectDTO();
        request.setTagIds(null);
        assertNull(request.getTagIds());
    }

    @Test
    void testEmptyTagIds() {
        ProjectDTO request = new ProjectDTO();
        request.setTagIds(new ArrayList<>());
        assertNotNull(request.getTagIds());
        assertTrue(request.getTagIds().isEmpty());
    }

    @Test
    void testVisibilityValues() {
        ProjectDTO privateRequest = new ProjectDTO();
        privateRequest.setVisibility(0);
        assertEquals(0, privateRequest.getVisibility());

        ProjectDTO publicRequest = new ProjectDTO();
        publicRequest.setVisibility(1);
        assertEquals(1, publicRequest.getVisibility());
    }
}
